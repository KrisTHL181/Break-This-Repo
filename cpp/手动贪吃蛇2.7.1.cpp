#include<iostream>
#include<cstdio>
#include<ctime>
#include<cstdlib>
#include<algorithm>
#include<queue>
#include<windows.h>
#include<conio.h>
#define ninf(x) ((x)==1000000000?-1:(x))
using namespace std;
void gotoxy(int,int);
void sethw();
void saveg(); 
void loadg();
void rcolor();
void scolor(int);
struct S{
	int x,y;
};
int g[32][32];
queue<S> dq;
int cl=0,mde=0,mmde=0,p=100,maxp[4],l=1,maxl[4],mint[4]={-1,1000000000,1000000000,1000000000};
int N=-1;
int main(){
	srand(time(0));
	gotoxy(0,0);
	sethw();
	cout<<"加载中";
	loadg();
	for(int i=1;i<=25;i++){
		cout<<".";
		Sleep(rand()%50+1);
	}
	start:
	scolor(cl);
	system("cls");
	gotoxy(29,0);
	cout<<"手动贪吃蛇2.7.1版本";
	gotoxy(32,1);
	cout<<"作者：张博雅";
	gotoxy(3,3);
	cout<<"规则："<<endl;
	cout<<"1、按wasd或上下左右移动，按q退出，按r重来，按空格暂停；"<<endl;
	cout<<"2、吃到食物可以增加长度；"<<endl;
	cout<<"3、碰墙可以到另一边；占满整个格子就胜利；"<<endl;
	cout<<"4、初始分数为x*20，每走1格分数减1，吃到食物可加分；"<<endl;
	cout<<"5、头碰到自己或分数为0就失败。"<<endl;
	gotoxy(1,10);
	cout<<"你的最高分："<<"	简单: "<<maxp[1]<<" ,	普通: "<<maxp[2]<<" ,	困难: "<<maxp[3]<<endl;
	gotoxy(1,11);
	cout<<"你的最大长度："<<"	简单: "<<maxl[1]<<" ,	普通: "<<maxl[2]<<" ,	困难: "<<maxl[3]<<endl;
	gotoxy(1,12);
	cout<<"你的最小时间："<<"	简单: "<<ninf(mint[1])<<" ,	普通: "<<ninf(mint[2])<<" ,	困难: "<<ninf(mint[3])<<endl;
	gotoxy(1,13);
	cout<<"(最小时间为-1代表未通关)"<<endl;
	gotoxy(0,16);
	cout<<"================================================================================";
	gotoxy(14,18);
	cout<<"s:开始游戏";
	gotoxy(34,18);
	cout<<"q:退出游戏";
	gotoxy(54,18);
	cout<<"v:保存记录";
	gotoxy(0,20);
	cout<<"================================================================================";
	gotoxy(0,22);
	cout<<"按上下键切换风格";
	gotoxy(0,24);
	cout<<"由于长度算法变化，2.3.3及之前的存档会不准确";
	gotoxy(0,26);
	cout<<"代码长度：350行";
	gotoxy(0,0);
	char c='m';
	while(c!='s'&&c!='q'&&c!='v'&&c!=72&&c!=80)c=getch();
	if(c=='q')return 0;
	else if(c==72){
		cl++;
		cl%=4;
		goto start;
	}
	else if(c==80){
		cl+=3;
		cl%=4;
		goto start;
	}
	else if(c=='v'){
		saveg();
		goto start;
	}
	system("cls");
	gotoxy(35,0);
	c='1';
	cout<<"难度选择";
	gotoxy(0,2);
	cout<<"经典："<<endl;
	cout<<"a：极简(4*4 debug)"<<endl;
	cout<<"e：简单(6*6)"<<endl;
	cout<<"m：普通(8*8)"<<endl;
	cout<<"h：困难(10*10)"<<endl;
	cout<<"s：自定义"<<endl;
	cout<<endl;
	cout<<"特殊："<<endl;
	cout<<"c：炫彩(8*8)"<<endl;
	while(c!='a'&&c!='e'&&c!='m'&&c!='h'&&c!='c'&&c!='s'&&c!='q')c=getch();
	if(c=='q')goto start;
	else if(c=='a'){
		N=4;
		mde=0;
		mmde=1;
	}
	else if(c=='e'){
		N=6;
		mde=1;
		mmde=0;
	}
	else if(c=='m'){
		N=8;
		mde=2;
		mmde=0;
	}
	else if(c=='h'){
		N=10;
		mde=3;
		mmde=0;
	}
	else if(c=='z'){
		N=20;
		mde=0;
		mmde=2;
		system("color C4");
	}
	else if(c=='c'){
		N=8;
		mde=0;
		mmde=3;
	}
	else if(c=='s'){
		gotoxy(0,17);
		cout<<"请输入边长（5<=x<=30）：";
		int xxx=-1;
		while(xxx<5||xxx>30)cin>>xxx;
		N=xxx;
		mde=0;
	}
	secst:
	system("cls"); 
	p=N*20;
	l=1;
	while(!dq.empty())dq.pop();
	S h;
	h.x=N/2;
	h.y=N/2;
	dq.push(h);
	for(int i=0;i<=N+1;i++){
		for(int j=0;j<=N+1;j++){
			if(i==0||i==N+1||j==0||j==N+1){
				gotoxy(i*2,j);
				cout<<"@";
			}
			g[i][j]=0;
		}
	}
	g[h.x][h.y]=1;
	int fx=rand()%N+1,fy=rand()%N+1;
	while(g[fx][fy]!=0||(fx==h.x&&fy==h.y)){
		fx=rand()%N+1;
		fy=rand()%N+1;
	}
	g[fx][fy]=2;
	gotoxy(fx*2,fy);
	cout<<"X";
	gotoxy(0,N+3);
	cout<<"分数:";
	if(p>=1000)cout<<p;
	else if(p>=100)cout<<"0"<<p;
	else if(p>=10)cout<<"00"<<p;
	else cout<<"000"<<p;
	gotoxy(0,N+4);
	cout<<"长度:"<<l;
	int tm=time(0);
	while(1){
		if(mmde==3)rcolor();
		h=dq.back();
		g[h.x][h.y]=1;
		gotoxy(h.x*2,h.y);
		cout<<"O";
		gotoxy(h.x*2,h.y);
		char c=getch();
		if(c=='w'||c==72)h.y--;
		else if(c=='s'||c==80)h.y++;
		else if(c=='a'||c==75)h.x--;
		else if(c=='d'||c==77)h.x++;
		else if(c=='q')goto start;
		else if(c=='r')goto secst;
		else if(c==' '){
			system("cls");
			int tmn=time(0);
			gotoxy(0,0);
			cout<<"正在暂停，按空格继续"<<endl;
			while(1)if(getch()==' ')break;
			tm+=(time(0)-tmn);
			system("cls");
			for(int i=0;i<=N+1;i++){
				for(int j=0;j<=N+1;j++){
					if(i==0||i==N+1||j==0||j==N+1){
						gotoxy(i*2,j);
						cout<<"@";
					}
					else{
						if(g[i][j]==1){
							gotoxy(i*2,j);
							cout<<"O";
						}
						else if(g[i][j]==2){
							gotoxy(i*2,j);
							cout<<"X";
						}
					}
				}
			}
			gotoxy(0,N+3);
			cout<<"分数:";
			if(p>=1000)cout<<p;
			else if(p>=100)cout<<"0"<<p;
			else if(p>=10)cout<<"00"<<p;
			else cout<<"000"<<p;
			gotoxy(0,N+4);
			cout<<"长度:"<<l;
			continue;
		}
		else continue;
		if(h.x>N)h.x=1;
		if(h.x<1)h.x=N;
		if(h.y>N)h.y=1;
		if(h.y<1)h.y=N;
		if(g[h.x][h.y]==0||dq.front().x==h.x&&dq.front().y==h.y){
			dq.push(h);
			h=dq.front();
			dq.pop();
			g[h.x][h.y]=0;
			gotoxy(h.x*2,h.y);
			cout<<" ";
		}
		else if(g[h.x][h.y]==1)break;
		else if(g[h.x][h.y]==2){
			if(l==N*N-1){
				system("cls");
				l++;
				gotoxy(0,0);
				cout<<"你赢了！！！按t返回主界面，按r再玩一次"<<endl;
				cout<<"分数:"<<p<<endl;
				cout<<"时间:"<<time(0)-tm<<endl;
				cout<<"长度:"<<l<<endl;
				maxp[mde]=max(maxp[mde],p);
				maxl[mde]=max(maxl[mde],l);
				mint[mde]=min(mint[mde],int(time(0)-tm));
				for(int i=1;i<=8;i++){
					rcolor();
					Sleep(150);
				}
				scolor(cl);
				char c='s';
				while(c!='r'&&c!='t')c=getch();
				if(c=='r')goto secst;
				system("cls");
				goto start;
			}
			dq.push(h);
			g[h.x][h.y]=0;
			int fx=rand()%N+1,fy=rand()%N+1;
			while(g[fx][fy]!=0||(fx==h.x&&fy==h.y)){
				fx=rand()%N+1;
				fy=rand()%N+1;
			}
			g[fx][fy]=2;
			gotoxy(fx*2,fy);
			cout<<"X";
			p+=N+1;
			l++;
		}
		p--;
		if(p==0)break;
		gotoxy(0,N+3);
		cout<<"分数:";
		if(p>=1000)cout<<p;
		else if(p>=100)cout<<"0"<<p;
		else if(p>=10)cout<<"00"<<p;
		else cout<<"000"<<p;
		gotoxy(0,N+4);
		cout<<"长度:"<<l;
	}
	if(mmde==2){
		while(1)cout<<char(rand()%128);
	}
	system("cls");
	gotoxy(0,0);
	cout<<"你输了！按t返回主界面，按r再玩一次"<<endl;
	cout<<"分数:"<<p<<endl;
	cout<<"时间:"<<time(0)-tm<<endl;
	cout<<"长度:"<<l<<endl;
	maxp[mde]=max(maxp[mde],p);
	maxl[mde]=max(maxl[mde],l);
	c='s';
	while(c!='r'&&c!='t')c=getch();
	if(c=='r')goto secst;
	goto start;
	return 0;
}
void rcolor(){
	char nnn[16]={'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
	char ppp[9]={'c','o','l','o','r',' ','%','%','\0'};
	ppp[6]=nnn[rand()%16];
	ppp[7]=nnn[rand()%16];
	system(ppp);
	return;
}
void scolor(int col){
	if(col==0)system("color 07");
	else if(col==1)system("color 0F");
	else if(col==2)system("color 70");
	else if(col==3)system("color F0");
	return;
}
void loadg(){
	FILE *f=NULL;
	f=fopen("game.gme","r");
	if(f==NULL)return;
	fscanf(f,"%d %d %d %d %d %d %d %d %d",&maxp[1],&maxp[2],&maxp[3],&maxl[1],&maxl[2],&maxl[3],&mint[1],&mint[2],&mint[3]);
	fclose(f);
	return;
}
void saveg(){
	FILE *f=fopen("game.gme","w");
	fprintf(f,"%d %d %d %d %d %d %d %d %d",maxp[1],maxp[2],maxp[3],maxl[1],maxl[2],maxl[3],mint[1],mint[2],mint[3]);
	fclose(f);
	return;
}
void gotoxy(int x,int y){
	HANDLE hdl;
	COORD crd;
	crd.X=x;
	crd.Y=y;
	hdl=GetStdHandle(STD_OUTPUT_HANDLE);
	SetConsoleCursorPosition(hdl,crd);
	return;
}
void sethw(){
	system("mode con: cols=80 lines=40");
	system("title 手动贪吃蛇");
}
