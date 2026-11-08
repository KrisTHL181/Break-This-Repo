import webview
import os
import json
import base64
import struct
from io import BytesIO
from PIL import Image
import zipfile
import tempfile
import shutil
import atexit


class Api:
    def __init__(self):
        self.temp_dir = tempfile.mkdtemp()
        self.uploaded_images = []
        self._window = None  # 私有属性，避免 COM 错误
        atexit.register(self._cleanup_on_exit)

    def set_window(self, window):
        """设置窗口引用"""
        self._window = window

    def minimize_window(self):
        """最小化窗口"""
        if self._window:
            self._window.minimize()

    def close_window(self):
        """关闭窗口"""
        if self._window:
            self._window.destroy()

    def _cleanup_on_exit(self):
        """程序退出时自动清理临时文件"""
        try:
            shutil.rmtree(self.temp_dir, ignore_errors=True)
        except:
            pass

    def select_images(self):
        """打开文件对话框选择图片"""
        result = webview.windows[0].create_file_dialog(
            webview.OPEN_DIALOG,
            allow_multiple=True,
            file_types=('Image Files (*.png;*.jpg;*.jpeg;*.bmp;*.gif)',)
        )
        if result:
            images = []
            start_id = len(self.uploaded_images)
            for i, path in enumerate(result):
                img_info = self._process_image(path, start_id + i)
                if img_info:
                    images.append(img_info)
            self.uploaded_images.extend(images)
            return {'success': True, 'images': images, 'total': len(self.uploaded_images)}
        return {'success': False, 'message': '未选择文件'}

    def select_folder(self):
        """打开文件夹对话框选择目录"""
        result = webview.windows[0].create_file_dialog(webview.FOLDER_DIALOG)
        if result and len(result) > 0:
            folder_path = result[0]
            images = []
            start_id = len(self.uploaded_images)
            valid_extensions = ('.png', '.jpg', '.jpeg', '.bmp', '.gif')
            for i, filename in enumerate(os.listdir(folder_path)):
                if filename.lower().endswith(valid_extensions):
                    path = os.path.join(folder_path, filename)
                    img_info = self._process_image(path, start_id + len(images))
                    if img_info:
                        images.append(img_info)
            self.uploaded_images.extend(images)
            return {'success': True, 'images': images, 'total': len(self.uploaded_images)}
        return {'success': False, 'message': '未选择文件夹'}

    def _process_image(self, path, img_id=None):
        """处理图片文件，返回图片信息"""
        if img_id is None:
            img_id = len(self.uploaded_images)
        try:
            with Image.open(path) as img:
                img.thumbnail((100, 100))
                buffered = BytesIO()
                img.save(buffered, format="PNG")
                img_str = base64.b64encode(buffered.getvalue()).decode()

                return {
                    'id': img_id,
                    'path': path,
                    'name': os.path.basename(path),
                    'preview': f'data:image/png;base64,{img_str}'
                }
        except Exception as e:
            print(f"处理图片失败: {path}, 错误: {e}")
            return None

    def clear_images(self):
        """清除所有已添加的图片"""
        self.uploaded_images = []
        return {'success': True}

    def get_images(self):
        """获取当前已添加的图片列表（用于前后端状态同步）"""
        return {'success': True, 'images': self.uploaded_images, 'total': len(self.uploaded_images)}

    def remove_image(self, index):
        """移除指定索引的图片"""
        if 0 <= index < len(self.uploaded_images):
            self.uploaded_images.pop(index)
            # 重新编号
            for i, img in enumerate(self.uploaded_images):
                img['id'] = i
            return {'success': True, 'total': len(self.uploaded_images)}
        return {'success': False, 'message': '索引无效'}

    def _create_bmp_ico(self, icon_images, output_path):
        """手动构建BMP格式的ICO文件，确保Windows兼容性"""
        # ICO文件头
        ico_header = struct.pack('<HHH', 0, 1, len(icon_images))  # 保留, 类型(1=ICO), 图像数量

        # 计算目录项和数据偏移
        dir_entries = b''
        image_data_list = []
        data_offset = 6 + 16 * len(icon_images)  # 头(6字节) + 目录项(16字节/图像)

        for img in icon_images:
            width = img.width if img.width < 256 else 0
            height = img.height if img.height < 256 else 0

            # 将图像转换为BMP格式（包含XOR和AND掩码）
            if img.mode == 'RGBA':
                # 32位带透明
                bmp_data = self._create_32bit_bmp(img)
            else:
                # 24位无透明
                bmp_data = self._create_24bit_bmp(img)

            # 目录项: 宽度, 高度, 颜色数, 保留, 颜色平面, 位深度, 数据大小, 数据偏移
            dir_entries += struct.pack('<BBBBHHII',
                width, height, 0, 0, 1,  # 宽度, 高度, 颜色数, 保留, 颜色平面
                32 if img.mode == 'RGBA' else 24,  # 位深度
                len(bmp_data),  # 数据大小
                data_offset  # 数据偏移
            )

            image_data_list.append(bmp_data)
            data_offset += len(bmp_data)

        # 写入ICO文件
        with open(output_path, 'wb') as f:
            f.write(ico_header)
            f.write(dir_entries)
            for data in image_data_list:
                f.write(data)

    def _create_32bit_bmp(self, img):
        """创建32位BMP图像数据（BGRA格式）"""
        width, height = img.size
        # BMP数据: 位图信息头 + XOR掩码 + AND掩码
        # 对于32位图像，AND掩码是可选的，因为alpha通道已经包含透明度

        # BITMAPINFOHEADER (40字节)
        header = struct.pack('<IiiHHIIiiII',
            40,  # 头大小
            width,  # 宽度
            height * 2,  # 高度（XOR和AND掩码的总高度）
            1,  # 颜色平面
            32,  # 位深度
            0,  # 压缩方式（0=无压缩）
            0,  # 图像大小（可以为0）
            0, 0,  # 分辨率
            0, 0  # 颜色数
        )

        # XOR掩码（BGRA格式，自下而上）
        pixels = img.convert('RGBA').tobytes()
        # 转换为BGRA
        bgra = bytearray()
        for y in range(height - 1, -1, -1):  # 自下而上
            for x in range(width):
                idx = (y * width + x) * 4
                r, g, b, a = pixels[idx], pixels[idx + 1], pixels[idx + 2], pixels[idx + 3]
                bgra.extend([b, g, r, a])

        # 行填充（32位图像每行必须是4字节对齐）
        row_size = width * 4
        padding = (4 - row_size % 4) % 4

        xor_mask = bytearray()
        for y in range(height):
            row_start = y * width * 4
            xor_mask.extend(bgra[row_start:row_start + row_size])
            xor_mask.extend([0] * padding)

        # AND掩码（1位单色，用于透明）- 对于32位图像，可以省略或全0
        and_mask = bytes((height * ((width + 31) // 32) * 4))

        return header + bytes(xor_mask) + and_mask

    def _create_24bit_bmp(self, img):
        """创建24位BMP图像数据（BGR格式）"""
        width, height = img.size

        # BITMAPINFOHEADER (40字节)
        header = struct.pack('<IiiHHIIiiII',
            40,  # 头大小
            width,  # 宽度
            height * 2,  # 高度
            1,  # 颜色平面
            24,  # 位深度
            0,  # 压缩方式
            0,  # 图像大小
            0, 0,  # 分辨率
            0, 0  # 颜色数
        )

        # 转换为BGR格式
        rgb_img = img.convert('RGB')
        pixels = rgb_img.tobytes()

        # 行填充（24位图像每行必须是4字节对齐）
        row_size = width * 3
        padding = (4 - row_size % 4) % 4

        # XOR掩码（BGR格式，自下而上）
        xor_mask = bytearray()
        for y in range(height - 1, -1, -1):  # 自下而上
            for x in range(width):
                idx = (y * width + x) * 3
                r, g, b = pixels[idx], pixels[idx + 1], pixels[idx + 2]
                xor_mask.extend([b, g, r])
            xor_mask.extend([0] * padding)

        # AND掩码（全0，表示不透明）
        and_mask = bytes((height * ((width + 31) // 32) * 4))

        return header + bytes(xor_mask) + and_mask

    def generate_ico(self, sizes, color_mode='rgba'):
        """生成ICO文件"""
        if not self.uploaded_images:
            return {'success': False, 'message': '请先添加图片'}

        try:
            size_list = [int(s) for s in sizes if s]
            if not size_list:
                return {'success': False, 'message': '请选择至少一个尺寸'}

            results = []
            for img_info in self.uploaded_images:
                img = None
                icon_images = []
                try:
                    with Image.open(img_info['path']) as img:
                        if img.mode != 'RGBA':
                            img = img.convert('RGBA')
                        
                        for size in sorted(size_list, reverse=True):
                            resized = img.resize((size, size), Image.Resampling.LANCZOS)
                            if color_mode == 'rgb':
                                resized = resized.convert('RGB')
                            icon_images.append(resized)

                        output_name = os.path.splitext(img_info['name'])[0] + '.ico'
                        output_path = os.path.join(self.temp_dir, output_name)
                        self._create_bmp_ico(icon_images, output_path)

                        results.append({
                            'name': output_name,
                            'path': output_path,
                            'sizes': sorted(size_list, reverse=True)
                        })
                finally:
                    for icon_img in icon_images:
                        icon_img.close()

            return {'success': True, 'files': results}
        except Exception as e:
            import traceback
            return {'success': False, 'message': f'生成失败: {str(e)}\n{traceback.format_exc()}'}

    def save_ico(self, file_path, ico_files):
        """保存ICO文件到指定位置"""
        try:
            for ico_file in ico_files:
                src_path = ico_file['path']
                dst_path = os.path.join(file_path, ico_file['name'])
                shutil.copy2(src_path, dst_path)
            return {'success': True}
        except Exception as e:
            return {'success': False, 'message': f'保存失败: {str(e)}'}

    def package_zip(self, ico_files, save_path=None):
        """打包ICO文件为ZIP，可选保存到指定路径"""
        try:
            if save_path:
                # 保存到指定路径
                zip_path = os.path.join(save_path, 'icons.zip')
            else:
                # 保存到临时目录
                zip_path = os.path.join(self.temp_dir, 'icons.zip')

            with zipfile.ZipFile(zip_path, 'w', zipfile.ZIP_DEFLATED) as zf:
                for ico_file in ico_files:
                    zf.write(ico_file['path'], ico_file['name'])
            return {'success': True, 'path': zip_path}
        except Exception as e:
            return {'success': False, 'message': f'打包失败: {str(e)}'}

    def select_save_folder(self):
        """选择保存文件夹"""
        result = webview.windows[0].create_file_dialog(webview.FOLDER_DIALOG)
        if result and len(result) > 0:
            return {'success': True, 'path': result[0]}
        return {'success': False, 'message': '未选择文件夹'}

    def cleanup(self):
        """清理临时文件"""
        try:
            shutil.rmtree(self.temp_dir, ignore_errors=True)
        except:
            pass


def get_center_position(width, height):
    """计算窗口居中位置"""
    try:
        import ctypes
        # 获取屏幕宽高
        user32 = ctypes.windll.user32
        screen_width = user32.GetSystemMetrics(0)
        screen_height = user32.GetSystemMetrics(1)
        # 计算居中位置
        x = (screen_width - width) // 2
        y = (screen_height - height) // 2
        return x, y
    except:
        # 如果失败，返回默认位置
        return 100, 100


def main():
    api = Api()

    # 获取HTML文件路径
    html_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'web', 'index.html')

    # 计算居中位置
    window_width = 1200
    window_height = 800
    x, y = get_center_position(window_width, window_height)

    # 创建窗口（无边框模式）
    window = webview.create_window(
        'ICO 生成器',
        html_path,
        js_api=api,
        width=window_width,
        height=window_height,
        x=x,
        y=y,
        resizable=False,
        frameless=True,
        easy_drag=True
    )
    api.set_window(window)

    # 启动应用
    webview.start(debug=False)

    # 清理临时文件
    api.cleanup()


if __name__ == '__main__':
    main()
