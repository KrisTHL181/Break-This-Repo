import random
from collections import deque
from concurrent.futures import ThreadPoolExecutor, as_completed
import logging
import os

# 设置日志
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

def play_game():
    # 初始化扑克牌（去掉大小王）
    deck = list(range(1, 14)) * 4
    random.shuffle(deck)

    # 均分给A和B
    half = len(deck) // 2
    a_deck = deque(deck[:half])
    b_deck = deque(deck[half:])
    
    table = []
    a_turn = True  # A先出牌
    a_moves = 0
    b_moves = 0
    
    while a_deck and b_deck:
        if a_turn:
            card = a_deck.popleft()
            a_moves += 1
        else:
            card = b_deck.popleft()
            b_moves += 1
        
        table.append(card)
        a_turn = not a_turn
        
        # 检查是否有匹配
        for i in range(len(table) - 1):
            if table[i] == card:
                if a_turn:  # 刚出牌的是B，A收回
                    a_deck.extend(table[i:])
                else:       # 刚出牌的是A，B收回
                    b_deck.extend(table[i:])
                table = table[:i]
                break
    
    winner = 'A' if a_deck else 'B'
    return winner, a_moves, b_moves

def simulate_games_parallel(num_games, num_workers):
    a_wins = 0
    b_wins = 0
    a_moves_list = []
    b_moves_list = []
    
    with ThreadPoolExecutor(max_workers=num_workers) as executor:
        futures = [executor.submit(play_game) for _ in range(num_games)]
        
        for future in as_completed(futures):
            try:
                winner, a_moves, b_moves = future.result()
                a_moves_list.append(a_moves)
                b_moves_list.append(b_moves)
                if winner == 'A':
                    a_wins += 1
                else:
                    b_wins += 1
            except Exception as e:
                logging.error(f"Exception in future.result(): {e}")
            
    return a_wins, b_wins, a_moves_list, b_moves_list

# 模拟24000局游戏，使用24个工作线程
num_games = 24000
num_workers = 24
a_wins, b_wins, a_moves_list, b_moves_list = simulate_games_parallel(num_games, num_workers)

print(f"A赢了 {a_wins} 次")
print(f"B赢了 {b_wins} 次")

# 获取桌面路径
desktop_path = os.path.join(os.path.join(os.environ['USERPROFILE']), 'Desktop')

# 将出牌次数保存到桌面文件
a_moves_file = os.path.join(desktop_path, "a_moves_list.txt")
b_moves_file = os.path.join(desktop_path, "b_moves_list.txt")

with open(a_moves_file, "w") as a_file:
    for move in a_moves_list:
        a_file.write(f"{move}\n")

with open(b_moves_file, "w") as b_file:
    for move in b_moves_list:
        b_file.write(f"{move}\n")

print(f"A每局游戏出牌次数已保存到 {a_moves_file}")
print(f"B每局游戏出牌次数已保存到 {b_moves_file}")
