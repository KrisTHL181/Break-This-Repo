#include<iostream>
#include<cstdio>
#include<ctime>
#include<cstdlib>
#include<algorithm>
#include<queue>
#include<windows.h>
#include<conio.h>
using namespace std;
void gotoxy(int x,int y);
struct S{
	int x,y;
};
int g[22][22];
queue<S> dq;
int p=100,maxp=0,l=1;
int N=-1;
int main(){
	srand(time(0));
	gotoxy(3,0);
	cout<<"1.2.2 alpha版，如有bug请指出";
	gotoxy(10,1);
	cout<<"wasd移动，q退出";
	gotoxy(10,2);
	cout<<"蛇是O，食物是X";
	gotoxy(10,3);
	cout<<"作者：张博雅";
	Sleep(2000);
	gotoxy(0,4);
	cout<<"按s开始游戏";
	char sss='t';
	while(sss!='s')sss=getch(); 
	system("cls");
	gotoxy(0,0);
	cout<<"请输入游玩区域大小（5<=x<=20）：";
	while(N<5||N>20)cin>>N;
	system("cls");
	start:
	S h;
	h.x=N/2;
	h.y=N/2;
	dq.push(h);
	for(int i=0;i<=N+1;i++){
		for(int j=0;j<=N+1;j++){
			if(i==0||i==N+1||j==0||j==N+1){
				gotoxy(i,j);
				cout<<"@";
			}
			g[i][j]=0;
		}
	}
	g[h.x][h.y]=1;
	int fx=rand()%N+1,fy=rand()%N+1;
	while(g[fx][fy]!=0||(fx==h.x||fy==h.y)){
		fx=rand()%N+1;
		fy=rand()%N+1;
	}
	g[fx][fy]=2;
	gotoxy(fx,fy);
	cout<<"X";
	gotoxy(0,N+2);
	cout<<"最高分:"<<maxp;
	gotoxy(0,N+3);
	cout<<"分数:";
	if(p>=1000)cout<<p;
	else if(p>=100)cout<<"0"<<p;
	else if(p>=10)cout<<"00"<<p;
	else cout<<"000"<<p;
	int tm=time(0);
	while(1){
		h=dq.back();
		g[h.x][h.y]=1;
		gotoxy(h.x,h.y);
		cout<<"O";
		char c=getch();
		if(c=='w')h.y--;
		if(c=='s')h.y++;
		if(c=='a')h.x--;
		if(c=='d')h.x++;
		if(c=='q')return 0;
		if(h.x>N)h.x=1;
		if(h.x<1)h.x=N;
		if(h.y>N)h.y=1;
		if(h.y<1)h.y=N;
		if(g[h.x][h.y]==0){
			dq.push(h);
			h=dq.front();
			dq.pop();
			g[h.x][h.y]=0;
			gotoxy(h.x,h.y);
			cout<<" ";
		}
		else if(g[h.x][h.y]==1)break;
		else if(g[h.x][h.y]==2){
			dq.push(h);
			g[h.x][h.y]=0;
			int fx=rand()%N+1,fy=rand()%N+1;
			while(g[fx][fy]!=0||(fx==h.x&&fy==h.y)){
				fx=rand()%N+1;
				fy=rand()%N+1;
			}
			g[fx][fy]=2;
			gotoxy(fx,fy);
			cout<<"X";
			p+=N+1;
			l++;
		}
		if(l==N*N-1){
			system("cls");
			gotoxy(0,0);
			cout<<"你赢了！！！按r再玩一次"<<endl;
			cout<<"分数:"<<p<<endl;
			cout<<"时间:"<<time(0)-tm<<endl;
			cout<<"长度:"<<l<<endl;
			system("color F0");
			Sleep(500);
			system("color 0F");
			Sleep(500);
			system("color F0");
			Sleep(500);
			system("color 07");
			maxp=max(maxp,p);
			p=100;
			l=1;
			while(!dq.empty())dq.pop();
			char c='s';
			while(c!='r')c=getch();
			system("cls");
			goto start;
		}
		p--;
		if(p==0)break;
		gotoxy(0,N+3);
		cout<<"分数:";
		if(p>=1000)cout<<p;
		else if(p>=100)cout<<"0"<<p;
		else if(p>=10)cout<<"00"<<p;
		else cout<<"000"<<p;
	}
	system("cls");
	gotoxy(0,0);
	cout<<"你输了！按r再玩一次"<<endl;
	cout<<"分数:"<<p<<endl;
	cout<<"时间:"<<time(0)-tm<<endl;
	cout<<"长度:"<<l<<endl;
	maxp=max(maxp,p);
	p=100;
	l=1;
	while(!dq.empty())dq.pop();
	char c='s';
	while(c!='r')c=getch();
	system("cls");
	goto start;
	return 0;
}

void gotoxy(int x,int y){
	HANDLE hdl;
	COORD crd;
	crd.X=x;
	crd.Y=y;
	hdl=GetStdHandle(STD_OUTPUT_HANDLE);
	SetConsoleCursorPosition(hdl,crd);
}
