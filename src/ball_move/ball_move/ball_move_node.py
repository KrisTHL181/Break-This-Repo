
import rclpy
from rclpy.node import Node
from ball_poses.msg import BallPos
from geometry_msgs.msg import Twist
from turtlesim.msg import Pose
import math

class BallMoveNode(Node):
    def __init__(self, node_name):
        super().__init__(node_name)
        self.ball_subscriber_ = self.create_subscription(BallPos, "ball_pos", self.ball_subscriber_callback, 10)
        self.geo_subscriber_ = self.create_subscription(Pose, "turtle1/pose", self.geo_subscriber_callback, 10)
        self.publisher_ = self.create_publisher(Twist, "turtle1/cmd_vel", 10)
        self.k_ = 1.3
        self.max_speed_ = 2.5
        self.max_ang_speed_ = 4.0
        self.target_ball_ = 1
        self.desti_ = {1:(5.54,5.54), 2:(0,0), 3:(0,0), 4:(0,0)} #出生点
        self.start = 0 #未接收到小球位置信息就不动

    def ball_subscriber_callback(self, msg):
        self.desti_[msg.id] = (msg.x, msg.y) #将接收的消息以字典中元组值形式保存
        self.start = 1

    def geo_subscriber_callback(self, msg):
        if self.start == 1: #未接收到小球位置信息就不动
            if self.target_ball_ <= 3: #3个球都到达了之后停止移动
                d_x = self.desti_[self.target_ball_][0]
                d_y = self.desti_[self.target_ball_][1]
                distance = math.sqrt(
                    (d_x-msg.x)**2 + (d_y-msg.y)**2
                )
                if distance<=0.5:
                    self.get_logger().info(f"到达球：{self.target_ball_}, 当前坐标: x:{msg.x}, y:{msg.y}, 误差：{distance}") #到达目标球，打印日志
                    self.target_ball_+=1
                    if self.target_ball_ == 4:
                        return
                    d_x = self.desti_[self.target_ball_][0] #重来一遍
                    d_y = self.desti_[self.target_ball_][1]
                    distance = math.sqrt(
                        (d_x-msg.x)**2 + (d_y-msg.y)**2
                    )
                
                angle = math.atan2(d_y-msg.y, d_x-msg.x) - msg.theta
                #self.get_logger().warn(f"角度差：{angle}") #TODO：调试
                msg1 = Twist()

                if math.fabs(angle)<0.2:
                    msg1.linear.x = distance*self.k_
                elif math.fabs(angle)>=0.2 and math.fabs(angle)<0.3:
                    msg1.linear.x = distance*self.k_
                    msg1.angular.z = math.fabs(angle) #TODO：加不加绝对值好像都一样？
                else:
                    msg1.angular.z = math.fabs(angle)
                if msg1.linear.x>self.max_speed_:
                    msg1.linear.x = self.max_speed_
                if msg1.angular.z>self.max_ang_speed_:
                    msg1.angular.z = self.max_ang_speed_
                self.publisher_.publish(msg1)

def main():
    rclpy.init()
    node = BallMoveNode("ball_move")
    rclpy.spin(node)
    rclpy.shutdown()
    