import cv2

vc = cv2.VideoCapture('bad apple.flv')


rval = vc.isOpened()
if not rval:
    print('Video open fail!')
    exit()

mp = [[False for j in range(64)]for i in range(128)]

index = 0
f = open('./pic/bad_apple_full86.codeb', 'wb+')
while True:
    rval, frame = vc.read()
    if rval:
        # if index%2==0:
        #     index += 1
        #     continue
        frame = cv2.resize(frame, (86,64))
        frame = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        _, frame = cv2.threshold(frame, 127, 255, cv2.THRESH_BINARY)
        for i in range(86):
            for j in range(64):
                if frame[j][i] == 255:
                    if mp[i][j]==False:
                        # f.write((129).to_bytes(1,'little'))
                        f.write((i+128+21).to_bytes(1,'little'))
                        f.write((j+128).to_bytes(1,'little'))
                        mp[i][j]=True
                else:
                    if mp[i][j]==True:
                        # f.write((128).to_bytes(1,'little'))
                        f.write((i+128+21).to_bytes(1,'little'))
                        f.write((j).to_bytes(1,'little'))
                        mp[i][j]=False
        f.write((1).to_bytes(1,'little'))
                    
    else:
        break
    index += 1
# i =  255
# b = i.to_bytes(1, 'little')
# print(int.from_bytes(b,'little'))
f.write((2).to_bytes(1,'little'))
f.close()
print('Done!')