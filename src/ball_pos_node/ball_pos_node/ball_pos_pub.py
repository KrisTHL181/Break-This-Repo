
import rclpy
from rclpy.node import Node
from ball_poses.msg import BallPos
from rcl_interfaces.msg import SetParametersResult


class BallPosPub(Node):
    def __init__(self, node_name):
        super().__init__(node_name)
        self.publisher_ = self.create_publisher(BallPos, "ball_pos", 10)
        self.declare_parameter("freq", 5.0)
        self.freq_ = self.get_parameter("freq").value
        self.timer_ = self.create_timer(1 / self.freq_, self.timer_callback) #TODO
        self.add_on_set_parameters_callback(self.set_params_callback)

    def set_params_callback(self, params):
        for param in params:
            if param.value > 0.0:
                self.freq_ = param.value
                self.timer_.timer_period_ns = int((1.0 / self.freq_) * 1e9) #更新计时器周期
                self.get_logger().info(f"参数 {param.name} 被设置为 {param.value}")
            elif param.value <= 0.0:
                self.get_logger().warn(f"非法参数：{param.value}")
                return SetParametersResult(successful=False)
        return SetParametersResult(successful=True)
        


    def timer_callback(self):
        msg = BallPos()

        msg.id = 1
        msg.x = 2.0
        msg.y = 3.0
        self.publisher_.publish(msg)
        self.get_logger().info("发布球：id=1, x=2.0, y=3.0")

        msg.id = 2
        msg.x = 5.5
        msg.y = 8.0
        self.publisher_.publish(msg)
        self.get_logger().info("发布球：id=2, x=5.5, y=8.0")

        msg.id = 3
        msg.x = 9.0
        msg.y = 4.0
        self.publisher_.publish(msg)
        self.get_logger().info("发布球：id=3, x=9.0, y=4.0")
        self.get_logger().info("=========================")


def main():
    rclpy.init()
    node = BallPosPub("ball_pos_pub")
    rclpy.spin(node)
    rclpy.shutdown()