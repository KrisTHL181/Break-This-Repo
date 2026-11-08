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
void gotoxy(int x,int y);
void saveg(); 
void loadg();
struct S{
	int x,y;
};
int g[22][22];
queue<S> dq;
int mde=0,p=100,maxp[4],l=1,maxl[4],mint[4]={-1,1000000000,1000000000,1000000000};
int N=-1;
int main(){
	srand(time(0));
	gotoxy(0,0);
	cout<<"加载中";
	loadg();
	for(int i=1;i<=20;i++){
		cout<<".";
		Sleep(rand()%200+1);
	}
	start:
	system("cls");
	gotoxy(28,0);
	cout<<"手动贪吃蛇2.4.0版本";
	gotoxy(31,1);
	cout<<"作者：张博雅";
	gotoxy(3,3);
	cout<<"规则："<<endl;
	cout<<"1、按wasd移动，按q退出，按r重来，按空格暂停；碰墙可以到另一边；"<<endl;
	cout<<"2、吃到食物可以增加长度；"<<endl;
	cout<<"3、占满整个格子就胜利；"<<endl;
	cout<<"4、初始分数为x*20，每走1格分数减1，吃到食物可加分；"<<endl;
	cout<<"5、头碰到自己或分数为0就失败。"<<endl;
	gotoxy(1,10);
	cout<<"你的最高分："<<" e: "<<maxp[1]<<" , m: "<<maxp[2]<<" , h: "<<maxp[3]<<endl;
	gotoxy(1,11);
	cout<<"你的最大长度："<<" e: "<<maxl[1]<<" , m: "<<maxl[2]<<" , h: "<<maxl[3]<<endl;
	gotoxy(1,12);
	cout<<"你的最小时间："<<" e: "<<ninf(mint[1])<<" , m: "<<ninf(mint[2])<<" , h: "<<ninf(mint[3])<<endl;
	gotoxy(10,15);
	cout<<"s:开始游戏";
	gotoxy(30,15);
	cout<<"q:退出游戏";
	gotoxy(50,15);
	cout<<"v:保存记录";
	gotoxy(0,18);
	cout<<"注意：不要在压缩文件内保存存档";
	gotoxy(0,19);
	cout<<"由于长度算法变化，2.3.3及之前的存档会不准确";
	gotoxy(0,21);
	cout<<"代码长度：288行";
	gotoxy(0,0);
	char c='m';
	while(c!='s'&&c!='q'&&c!='v')c=getch();
	if(c=='q')return 0;
	else if(c=='v'){
		saveg();
		goto start;
	}
	system("cls");
	gotoxy(35,0);
	c='t';
	cout<<"难度选择";
	gotoxy(0,2);
	cout<<"a：极简(4*4 debug)"<<endl;
	cout<<"e：简单(6*6)"<<endl;
	cout<<"m：普通(8*8)"<<endl;
	cout<<"h：困难(10*10)"<<endl; 
	cout<<"s：自定义"<<endl;
	while(c!='a'&&c!='e'&&c!='m'&&c!='h'&&c!='s')c=getch();
	if(c=='a'){
		N=4;
		mde=0;
	}
	else if(c=='e'){
		N=6;
		mde=1;
	}
	else if(c=='m'){
		N=8;
		mde=2;
	}
	else if(c=='h'){
		N=10;
		mde=3;
	}
	else if(c=='s'){
		gotoxy(0,16);
		cout<<"请输入边长（5<=x<=20）：";
		int xxx=-1;
		while(xxx<5||xxx>20)cin>>xxx;
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
		h=dq.back();
		g[h.x][h.y]=1;
		gotoxy(h.x*2,h.y);
		cout<<"O";
		gotoxy(h.x*2,h.y);
		char c=getch();
		if(c=='w')h.y--;
		else if(c=='s')h.y++;
		else if(c=='a')h.x--;
		else if(c=='d')h.x++;
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
				system("color F0");
				Sleep(500);
				system("color 0F");
				Sleep(500);
				system("color F0");
				Sleep(500);
				system("color 07");
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
