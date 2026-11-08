"""Bounded-process, local Vision OCR for lock digits. No screen capture here.

API: https://developer.apple.com/documentation/vision/vnrecognizetextrequest
Input/output are JSON over pipes; only tightly cropped lock faces are processed.
"""
import base64
import io
import json
import sys


def recognize(encoded):
    import Foundation
    import Quartz
    import Vision
    import numpy as np
    import cv2
    from PIL import Image, ImageOps
    image=Image.open(io.BytesIO(base64.b64decode(encoded))).convert('RGB')
    a=np.asarray(image).astype('int16')
    white=(a.min(2)>160)&(a.max(2)-a.min(2)<55)
    _,labels,stats,_=cv2.connectedComponentsWithStats(white.astype('uint8'),8)
    keep=[i for i,s in enumerate(stats[1:],1) if s[3]>=image.height*.4 and s[4]>image.width*image.height*.02]
    white=np.isin(labels,keep)
    # The raw face and isolated white digits must give the same integer. Never
    # turn letters into digits or choose a lower-confidence alternate silently.
    binary=Image.fromarray(np.where(white,0,255).astype('uint8')).convert('RGB')
    values=[]
    for original in (image,binary):
        crop=ImageOps.expand(original.resize((image.width*6,image.height*6)),border=25,fill='white')
        data=io.BytesIO();crop.save(data,format='PNG');raw=data.getvalue()
        source=Quartz.CGImageSourceCreateWithData(Foundation.NSData.dataWithBytes_length_(raw,len(raw)),None)
        cg=Quartz.CGImageSourceCreateImageAtIndex(source,0,None)
        request=Vision.VNRecognizeTextRequest.alloc().init()
        request.setRecognitionLevel_(Vision.VNRequestTextRecognitionLevelAccurate)
        request.setUsesCPUOnly_(True)
        request.setUsesLanguageCorrection_(False);request.setRecognitionLanguages_(['en-US'])
        handler=Vision.VNImageRequestHandler.alloc().initWithCGImage_orientation_options_(cg,1,None)
        ok,error=handler.performRequests_error_([request],None)
        if not ok:raise ValueError(str(error))
        candidates=[o.topCandidates_(1)[0] for o in request.results() if o.topCandidates_(1)]
        if len(candidates)!=1:raise ValueError('锁上数字不完整')
        text=str(candidates[0].string()).strip()
        if not text.isascii() or not text.isdecimal() or not 1<=int(text)<=999 or candidates[0].confidence()<.5:
            raise ValueError('锁上数字不确定')
        values.append(int(text))
    if values[0]!=values[1]:raise ValueError('两次锁数字识别不一致')
    return values[0]


if __name__=='__main__':
    try:
        print(json.dumps({'counts':[recognize(s) for s in json.load(sys.stdin)]}))
    except Exception as error:
        print(json.dumps({'error':str(error)}))
        sys.exit(1)
