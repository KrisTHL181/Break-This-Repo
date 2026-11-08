"""Continuous iPhone Mirroring video frames; no screenshot API or audio capture.

SCStream follows Apple's Capturing screen content in macOS sample.
Frames are kept in memory only.
"""
import threading
import time
import objc
import Quartz as Q
import CoreMedia as CM
import ScreenCaptureKit as SC
from Foundation import NSObject
from PIL import Image


class CaptureError(RuntimeError):
    pass


class PhoneVideoOutput(NSObject, protocols=[objc.protocolNamed('SCStreamOutput'),
                                           objc.protocolNamed('SCStreamDelegate')]):
    def stream_didOutputSampleBuffer_ofType_(self, stream, sample, kind):
        if kind != SC.SCStreamOutputTypeScreen or not CM.CMSampleBufferIsValid(sample):
            return
        owner = self.owner
        if stream != owner.stream:
            return
        try:
            now = time.monotonic()
            attachments = CM.CMSampleBufferGetSampleAttachmentsArray(sample, False)
            status = attachments[0].get(SC.SCStreamFrameInfoStatus) if attachments else None
            if status == SC.SCFrameStatusIdle:
                # Idle is a live compositor report that the image is unchanged.
                with owner.condition:
                    if owner.image is not None:
                        owner.captured_at = now
                        owner.sequence += 1
                        owner.condition.notify_all()
                return
            if status not in (None, SC.SCFrameStatusComplete):
                return
            buffer = CM.CMSampleBufferGetImageBuffer(sample)
            if buffer is None:
                return
            width, height = Q.CVPixelBufferGetWidth(buffer), Q.CVPixelBufferGetHeight(buffer)
            stride = Q.CVPixelBufferGetBytesPerRow(buffer)
            if Q.CVPixelBufferLockBaseAddress(buffer, Q.kCVPixelBufferLock_ReadOnly):
                return
            try:
                raw = Q.CVPixelBufferGetBaseAddress(buffer).as_buffer(stride*height)
                image = Image.frombytes('RGBA', (width,height), raw, 'raw', 'BGRA', stride).convert('RGB')
            finally:
                Q.CVPixelBufferUnlockBaseAddress(buffer, Q.kCVPixelBufferLock_ReadOnly)
            timestamp = CM.CMTimeGetSeconds(CM.CMSampleBufferGetPresentationTimeStamp(sample))
            captured_at = timestamp if 0 <= now-timestamp < 10 else now
            with owner.condition:
                owner.image, owner.captured_at = image, captured_at
                owner.sequence += 1
                owner.condition.notify_all()
        except Exception as error:
            with owner.condition:
                owner.error = f'视频帧读取失败：{error}'
                owner.condition.notify_all()

    def stream_didStopWithError_(self, stream, error):
        if stream != self.owner.stream:
            return
        with self.owner.condition:
            self.owner.error = f'镜像视频流中断：{error}'
            self.owner.condition.notify_all()


class PhoneCapture:
    def __init__(self):
        self.condition = threading.Condition()
        self.stream = None
        self.image = None
        self.error = None
        self.sequence = 0
        self.delivered = -1
        self.captured_at = 0
        self.geometry_at = 0

    def start(self):
        if not Q.CGPreflightScreenCaptureAccess():
            raise CaptureError('请为 Python 开启屏幕录制权限，然后重启绿点工具')
        event, result = threading.Event(), {}

        def received(content, error):
            result.update(content=content, error=error)
            event.set()

        SC.SCShareableContent.getShareableContentExcludingDesktopWindows_onScreenWindowsOnly_completionHandler_(True, True, received)
        if not event.wait(8):
            raise CaptureError('读取镜像窗口超时')
        if result['error']:
            raise CaptureError(str(result['error']))
        windows = [w for w in result['content'].windows() if w.owningApplication()
                   and w.owningApplication().bundleIdentifier() == 'com.apple.ScreenContinuity'
                   and w.frame().size.width > 200 and w.frame().size.height > 400]
        if not windows:
            raise CaptureError('请打开并连接 iPhone 镜像')
        window = max(windows, key=lambda w:w.frame().size.width*w.frame().size.height)
        frame = window.frame()
        self.frame = {'id':int(window.windowID()), 'x':float(frame.origin.x), 'y':float(frame.origin.y),
                      'width':float(frame.size.width), 'height':float(frame.size.height)}
        config = SC.SCStreamConfiguration.alloc().init()
        config.setWidth_(round(frame.size.width))
        config.setHeight_(round(frame.size.height))
        config.setPixelFormat_(Q.kCVPixelFormatType_32BGRA)
        config.setMinimumFrameInterval_(CM.CMTimeMake(1,30))
        config.setQueueDepth_(3)
        config.setShowsCursor_(False)
        config.setCapturesAudio_(False)
        if hasattr(config,'setCaptureMicrophone_'):
            config.setCaptureMicrophone_(False)
        config.setIgnoreShadowsSingleWindow_(True)
        self.output = PhoneVideoOutput.alloc().init()
        self.output.owner = self
        content_filter = SC.SCContentFilter.alloc().initWithDesktopIndependentWindow_(window)
        self.stream = SC.SCStream.alloc().initWithFilter_configuration_delegate_(content_filter, config, self.output)
        ok, error = self.stream.addStreamOutput_type_sampleHandlerQueue_error_(self.output, SC.SCStreamOutputTypeScreen, None, None)
        if not ok:
            self.close()
            raise CaptureError(str(error))
        event.clear()

        def started(error):
            result['error'] = error
            event.set()

        self.stream.startCaptureWithCompletionHandler_(started)
        if not event.wait(8) or result.get('error'):
            self.close()
            raise CaptureError(str(result.get('error') or '视频流启动超时'))

    def geometry(self):
        now = time.monotonic()
        if now-self.geometry_at < .3:
            return
        windows = Q.CGWindowListCopyWindowInfo(Q.kCGWindowListOptionOnScreenOnly,Q.kCGNullWindowID)
        w = next((w for w in windows if w.get(Q.kCGWindowNumber) == self.frame['id']),None)
        if w is None:
            raise CaptureError('镜像窗口不可见，请恢复镜像窗口')
        b = w[Q.kCGWindowBounds]
        if abs(b['Width']-self.frame['width'])>1 or abs(b['Height']-self.frame['height'])>1:
            self.close()
            raise CaptureError('镜像尺寸改变，正在重新连接视频流')
        self.frame.update(x=float(b['X']), y=float(b['Y']))
        self.geometry_at = now

    def capture(self):
        if self.error:
            message = self.error
            self.close()
            raise CaptureError(message)
        if self.stream is None:
            self.start()
        self.geometry()
        deadline = time.monotonic()+2
        with self.condition:
            while self.sequence == self.delivered or self.image is None:
                if self.error:
                    raise CaptureError(self.error)
                remaining = deadline-time.monotonic()
                if remaining <= 0:
                    raise CaptureError('镜像没有新的视频帧，请检查连接')
                self.condition.wait(remaining)
            self.delivered = self.sequence
            image, captured_at = self.image.copy(), self.captured_at
        image.info['captured_at'] = captured_at
        return image, dict(self.frame)

    def close(self):
        stream, self.stream = self.stream, None
        if stream is not None:
            stream.stopCaptureWithCompletionHandler_(lambda error:None)
        self.image = None
        self.error = None
        self.delivered = -1
