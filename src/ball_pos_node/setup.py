from setuptools import find_packages, setup

package_name = 'ball_pos_node'

setup(
    name=package_name,
    version='0.0.0',
    packages=find_packages(exclude=['test']),
    data_files=[
        ('share/ament_index/resource_index/packages',
            ['resource/' + package_name]),
        ('share/' + package_name, ['package.xml']),
    ],
    install_requires=['setuptools'],
    zip_safe=True,
    maintainer='hakashin',
    maintainer_email='2687289881@qq.com',
    description='TODO: Package description',
    license='Apache-2.0',
    extras_require={
        'test': [
            'pytest',
        ],
    },
    entry_points={
        'console_scripts': [
            "ball_pos_pub = ball_pos_node.ball_pos_pub:main",
            "ball_pos_sub = ball_pos_node.ball_pos_sub:main",
        ],
    },
)
