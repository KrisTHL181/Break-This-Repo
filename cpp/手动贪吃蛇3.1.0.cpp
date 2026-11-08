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
void random_color();
void scolor(int);
void settings();
bool sure();
struct S{
	int x,y;
};
int g[32][32];
queue<S> dq;
bool autosave=1;
int color=0,mde=0,spe_mode=0,p=100,max_point[4],length=1,max_length[4],min_time[4]={-1,1000000000,1000000000,1000000000};
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
	if(autosave)saveg();
	scolor(color);
	system("cls");
	gotoxy(29,0);
	cout<<"手动贪吃蛇3.1.0版本";
	gotoxy(28,1);
	cout<<"作者：19ty84(XSsMCiB)";
	gotoxy(3,3);
	cout<<"规则："<<endl;
	cout<<"1、在游戏中按wasd或上下左右移动，按q结束游戏，按e瞬间退出，按空格暂停；"<<endl;
	cout<<"2、吃到食物可以增加长度；"<<endl;
	cout<<"3、占满整个格子就胜利；"<<endl;
	cout<<"4、初始分数为x*20，每走1格分数减1，吃到食物可加分；"<<endl;
	cout<<"5、头碰到墙、头碰到自己或分数为0就失败。"<<endl;
	gotoxy(1,10);
	cout<<"你的最高分："<<"	简单: "<<max_point[1]<<" ,	普通: "<<max_point[2]<<" ,	困难: "<<max_point[3]<<endl;
	gotoxy(1,11);
	cout<<"你的最大长度："<<"	简单: "<<max_length[1]<<" ,	普通: "<<max_length[2]<<" ,	困难: "<<max_length[3]<<endl;
	gotoxy(1,12);
	cout<<"你的最小时间："<<"	简单: "<<ninf(min_time[1])<<" ,	普通: "<<ninf(min_time[2])<<" ,	困难: "<<ninf(min_time[3])<<endl;
	gotoxy(1,13);
	cout<<"(最小时间为-1代表未通关)"<<endl;
	gotoxy(0,16);
	cout<<"================================================================================";
	gotoxy(10,18);
	cout<<"s:开始游戏";
	gotoxy(35,18);
	cout<<"q:退出游戏";
	gotoxy(60,18);
	cout<<"w:一些设置";
	gotoxy(0,20);
	cout<<"================================================================================";
	gotoxy(0,24);
	cout<<"由于规则变化，2.8.0及之前的存档会不准确";
	gotoxy(0,26);
	cout<<"代码长度：426行";
	gotoxy(0,0);
	char c='m';
	while(c!='s'&&c!='w'&&c!='q'&&c!=72&&c!=80)c=getch();
	if(c=='w'){
		settings();
		goto start;
	}
	if(c=='q')return 0;
	else if(c==72){
		color++;
		color%=4;
		goto start;
	}
	else if(c==80){
		color+=3;
		color%=4;
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
		spe_mode=1;
	}
	else if(c=='e'){
		N=6;
		mde=1;
		spe_mode=0;
	}
	else if(c=='m'){
		N=8;
		mde=2;
		spe_mode=0;
	}
	else if(c=='h'){
		N=10;
		mde=3;
		spe_mode=0;
	}
	else if(c=='c'){
		N=8;
		mde=0;
		spe_mode=3;
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
	length=1;
	while(!dq.empty())dq.pop();
	S h;
	h.x=N/2;
	h.y=N/2;
	int lastx=-1,lasty=-1,lsx=-1,lsy=-1;
	bool frt=1;
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
	cout<<"长度:"<<length;
	gotoxy(0,N+6);
	cout<<"wasd或上下左右键移动"<<endl;
	cout<<"q退出，e瞬间退出，r重来，空格暂停"<<endl;
	int tm=time(0);
	while(1){
		if(spe_mode==3)random_color();
		h=dq.back();
		g[h.x][h.y]=1;
		gotoxy(h.x*2,h.y);
		cout<<"O";
		gotoxy(h.x*2,h.y);
		char c=getch();
		if(c=='w'||c==72){
			if(length!=1&&(h.y+N-2)%N+1==lasty)continue;
			h.y--;
		}
		else if(c=='s'||c==80){
			if(length!=1&&h.y%N+1==lasty)continue;
			h.y++;
		}
		else if(c=='a'||c==75){
			if(length!=1&&(h.x+N-2)%N+1==lastx)continue;
			h.x--;
		}
		else if(c=='d'||c==77){
			if(length!=1&&h.x%N+1==lastx)continue;
			h.x++;
		}
		else if(c=='q')break;
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
			cout<<"长度:"<<length;
			continue;
		}
		else continue;
		if(h.x>N||h.x<1||h.y>N||h.y<1)break;
		lastx=lsx;
		lasty=lsy;
		lsx=h.x;
		lsy=h.y;
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
			if(length==N*N-1){
				system("cls");
				length++;
				gotoxy(0,0);
				cout<<"你赢了！！！按t返回主界面，按r再玩一次"<<endl;
				cout<<"分数:"<<p<<endl;
				cout<<"时间:"<<time(0)-tm<<endl;
				cout<<"长度:"<<length<<endl;
				max_point[mde]=max(max_point[mde],p);
				max_length[mde]=max(max_length[mde],length);
				min_time[mde]=min(min_time[mde],int(time(0)-tm));
				for(int i=1;i<=8;i++){
					random_color();
					Sleep(150);
				}
				scolor(color);
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
			length++;
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
		cout<<"长度:"<<length;
	}
	if(spe_mode==2){
		while(1)cout<<char(rand()%128);
	}
	system("cls");
	gotoxy(0,0);
	cout<<"你输了！按t返回主界面，按r再玩一次"<<endl;
	cout<<"分数:"<<p<<endl;
	cout<<"时间:"<<time(0)-tm<<endl;
	cout<<"长度:"<<length<<endl;
	max_point[mde]=max(max_point[mde],p);
	max_length[mde]=max(max_length[mde],length);
	c='s';
	while(c!='r'&&c!='t')c=getch();
	if(c=='r')goto secst;
	goto start;
	return 0;
}
bool sure(){
	system("cls"); 
	cout<<"确定吗？输入 SuRe 来确定（注意大小写）";
	string suresuresure;
	cin>>suresuresure;
	if(suresuresure=="SuRe")return 1;
	else{
		cout<<"错误！"<<endl;
		Sleep(1000);
		return 0; 
	}
}
void settings(){
	dam:
	system("cls");
	gotoxy(0,0);
	cout<<"设置"<<endl;
	cout<<"按v存档"<<endl;
	cout<<"按上下键切换风格"<<endl;
	cout<<"按a取消或恢复自动保存（自动保存："<<(autosave?"开":"关")<<"）"<<endl;
	cout<<"按f清除所有存档"<<endl;
	cout<<"按q键退出"<<endl;
	char c='1';
	while(c!='v'&&c!=72&&c!=80&&c!='a'&&c!='f'&&c!='q')c=getch();
	if(c=='v'){
		saveg();
		goto dam;
	}
	else if(c==72){
		color++;
		color%=4;
		scolor(color);
		if(autosave)saveg();
		goto dam;
	}
	else if(c==80){
		color+=3;
		color%=4;
		scolor(color);
		if(autosave)saveg();
		goto dam;
	}
	else if(c=='a'){
		autosave=!autosave;
		if(autosave)saveg();
		goto dam;
	}
	else if(c=='f'){
		if(sure()){
			max_point[1]=max_point[2]=max_point[3]=0;
			max_length[1]=max_length[2]=max_length[3]=0;
			min_time[1]=min_time[2]=min_time[3]=1000000000;
			if(autosave)saveg();
		}
		goto dam;
	}
	else if(c=='q')return;
	return;
}
void random_color(){
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
	fscanf(f,"%d %d %d %d %d %d %d %d %d %d %d",&max_point[1],&max_point[2],&max_point[3],&max_length[1],&max_length[2],&max_length[3],&min_time[1],&min_time[2],&min_time[3],&color,&autosave);
	fclose(f);
	return;
}
void saveg(){
	system("cls");
	gotoxy(0,0);
	cout<<"保存中..."<<endl;
	FILE *f=fopen("game.gme","w");
	fprintf(f,"%d %d %d %d %d %d %d %d %d %d %d",max_point[1],max_point[2],max_point[3],max_length[1],max_length[2],max_length[3],min_time[1],min_time[2],min_time[3],color,autosave);
	fclose(f);
	cout<<"保存成功！";
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
