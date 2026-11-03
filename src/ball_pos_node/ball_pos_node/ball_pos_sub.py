
import rclpy
from rclpy.node import Node
from ball_poses.msg import BallPos


class BallPosSub(Node):
    def __init__(self, node_name):
        super().__init__(node_name)
        self.subscriber_ = self.create_subscription(BallPos, "ball_pos", self.subscriber_callback, 10)
        

    def subscriber_callback(self, msg):
        self.get_logger().info(f"id:{msg.id}, x:{msg.x}, y:{msg.y}")


def main():
    rclpy.init()
    node = BallPosSub("ball_pos_sub")
    rclpy.spin(node)
    rclpy.shutdown()