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
int p=1;
int main(){
	srand(time(0));
	start:
	S h;
	h.x=10;
	h.y=10;
	dq.push(h);
	for(int i=0;i<=21;i++){
		for(int j=0;j<=21;j++){
			if(i==0||i==21||j==0||j==21){
				gotoxy(i,j);
				cout<<"@";
			}
			g[i][j]=0;
		}
	}
	int fx=rand()%20+1,fy=rand()%20+1;
	g[fx][fy]=2;
	gotoxy(fx,fy);
	cout<<"X";
	gotoxy(10,22);
	cout<<"wasd移动";
	gotoxy(8,23);
	cout<<"作者：张博雅";
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
		if(h.x>20)h.x=1;
		if(h.x<1)h.x=20;
		if(h.y>20)h.y=1;
		if(h.y<1)h.y=20;
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
			int fx=rand()%20+1,fy=rand()%20+1;
			while(g[fx][fy]!=0){
				fx=rand()%20+1;
				fy=rand()%20+1;
			}
			g[fx][fy]=2;
			gotoxy(fx,fy);
			cout<<"X";
			p++;
		}
	}
	system("cls");
	gotoxy(0,2);
	cout<<"you lose!"<<endl;
	cout<<"point:"<<p<<endl;
	p=1;
	while(!dq.empty())dq.pop();
	system("pause");
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
