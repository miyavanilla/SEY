import java.awt.Color;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JFrame;
import javax.swing.JPanel;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URI;

import javax.sound.sampled.*;//音ならす用


//import sun.security.provider.JavaKeyStore.CaseExactJKS;
 
public class Billard4K extends JPanel implements Runnable, MouseListener, MouseMotionListener {
   
    // GAME STATES
    public final int WAITING_TO_START = 0;
    public final int WAITING_TO_HIT = 1;
    public final int MOVING = 2;
    public final int FINISHING = 3;
   
    public int state = 0;
   
    // TABLE
    double hR;//穴の半径
    double[] tableX;
    double[] tableY;
    double[] holesX;
    double[] holesY;
   
    // BALLS
    public int nballs;
    public int nBallsOn;
    double[] x;
    double[] y;
    double[] vx;
    double[] vy;
    double[] nextX;
    double[] nextY;
    double[] nextVx;
    double[] nextVy;
    boolean[] borderCollision;
    boolean[][] collision;
    boolean[] onTable;
    double r = 10;
   
    // RENDERING
    Image backBuffer;
    Image backGround;
   
    // MOUSE
    int mX;
    int mY;
    int mXPost;
    int mYPost;
    boolean clicked;
   
    // STICK
    public final int MAX_STRENGTH = 1000;
    int sL = 300;
    int actualStep = 0;
   
    float ballcolors[][] = new float[][]{{1.0f, 1.0f, 0.0f}, 
    									 {0.0f, 0.0f, 1.0f}, 
    									 {1.0f, 0.0f, 0.0f},
    									 {1.0f, 0.0f, 1.0f},
    									 
    									 {1.0f, 0.7f, 0.0f},
    									 {0.0f, 1.0f, 0.0f},
    									 {0.5f, 0.0f, 0.0f},
    									 {0.0f, 0.0f, 0.0f},
    									 {1.0f, 1.0f, 0.5f}};
    boolean order = false;//ボールを落とす順番を指定するか
    int current = 0;//ボールを落とした順番
    int clicktimes = 0;//ボールを打った回数
    
    boolean retry = true;//白いボールの数に制限をつけるか
    int retrynum = 3;//白いボールの数 カウント用
    int RETRYNUM = 3;//白いボールの数 初期値
    
    boolean gameover = false;//ゲームオーバー(タイトルに戻る)
    boolean boomessage = false;//ゲームオーバーしたら
    boolean compmessage = false;
    int indivimes = 0;
    
    //動画関連
    String[] uriString = new String[]{"https://drive.google.com/file/d/0B2qGSJvCoOjhYnNqektMV2lKUjA/view?usp=sharing",
    "http://www.amazon.co.jp/","http://www.nifty.com/","https://twitter.com/",
    "https://www.facebook.com","https://scholar.google.co.jp/","http://ci.nii.ac.jp/",
    "https://instagram.com/","https://www.whatsapp.com/?l=ja"
    };
    
    //サウンド一式
    AudioFormat format = null;
    DataLine.Info info = null;
    Clip line = null;
    File audioFile = null;
    String startsound = "welcome.wav";
    String bill1 = "billiard-ball1.wav";
    String break1 = "billiard-ball1.wav";
    String pocket1 = "billiard-pocket1.wav";
    String shot1 = "billiard-shot1.wav";
    
    boolean breakshot = true;//最初の一発か
    
    public Billard4K() {
        super();
        this.setBounds(50, 50, 700, 350);
        //this.setResizable(false);
        //this.setUndecorated(true);
        //this.setVisible(true);
 
        JFrame f = new JFrame("Billiard4K");        
        f.add(this);
        f.setBounds(0, 0, 700, 380);
        f.setResizable(false);
        f.setVisible(true);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
       
        this.requestFocus();
       
        init();
    }
   
    public void init() {
       
        initTable();
        initBalls();
       
        backBuffer = this.createImage(this.getWidth(), this.getHeight());
        //gBackBuffer = backBuffer.getGraphics();
        //gBackBuffer.setFont(new Font("Courier", Font.BOLD, 20));
       
        createBackGround();
 
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
       
        //音のテスト
        //サウンドファイルを読み込んで再生する
        // AIFF-C、AIFF、AU、SND、WAV　の各形式が利用可能
        
        setSound(startsound);
        //音のテストここまで
        
        start();
    }
   
   
    public void initTable() {
       
        //hR = 16;//デフォルト
    	hR = 22;
       
        tableX = new double[] {
          40,
          this.getWidth()-40
        };
       
        tableY = new double[] {
          tableX[0],
          this.getHeight()-tableX[0]
        };
       
        holesX = new double[] {
          tableX[0] + 20,
          this.getWidth()/2,
          tableX[1]-20
        };
       
        holesY = new double[] {
          tableY[0] + 20,
          this.tableY[1]-20
        };        
    }
   
   
    public void initBalls() {
        nballs = 10;//ボール半径？//10でナインボールに
        x = new double[nballs];//現在地
        y = new double[nballs];
        vx = new double[nballs];//動く量
        vy = new double[nballs];
        nextX = new double[nballs];
        nextY = new double[nballs];
        nextVx = new double[nballs];
        nextVy = new double[nballs];
        borderCollision = new boolean[nballs];
        collision = new boolean[nballs][nballs];
        onTable = new boolean[nballs];
       
        setBalls();
        setWhiteBall();    
       
    }
   
   
    public void setWhiteBall() {
        x[0] = 1.0 *(holesX[2]-holesX[0]) / 3.0;
        y[0] = this.getHeight() / 2;
        vx[0] = 0;
        vy[0] = 0;
       
        boolean collisions = false;
        boolean recolocate = false;
       
        do {
            collisions = false;
            recolocate = false;
           
            for (int i=1; !collisions && i<nballs; ++i) {
                collisions = isBallsCollisionPre(0, i);
            }
           
            if (collisions) {
                recolocate = true;
                y[0] -= r;
                if (y[0]<holesY[0]) y[0] = holesY[1]-r;
            }
        } while (recolocate);
       
        onTable[0] = true;
    }
   
   
    public void setBalls() {
        int ball=1;
        int line = nballs/5;
        nBallsOn = nballs - 1;
        final double mul = Math.sqrt(3.5);
        for (int col=0; col<line; ++col) {//col=何列並べるか
            double xN = this.getWidth()*2.0/3.0+col* mul *r;
            double yN = this.getHeight()/2-col*r;
            for (int row=0; row<=col; ++row) {
                x[ball] = xN;
                y[ball] = yN;
                vx[ball] = 0;
                vy[ball] = 0;
                onTable[ball] = true;
               
                yN += 2*r;
                ++ball;
            }
        }
        for (int col=0; col<line+1; ++col) {
            double xN = this.getWidth()*2.0/3.0+(line*2-col)* mul *r;
            double yN = this.getHeight()/2-col*r;
            for (int row=0; row<=col; ++row) {
                x[ball] = xN;
                y[ball] = yN;
                vx[ball] = 0;
                vy[ball] = 0;
                onTable[ball] = true;
               
                yN += 2*r;
                ++ball;
            }
        }
    }
   
    public void createBackGround() {
        backGround = this.createImage(this.getWidth(), this.getHeight());
        Graphics g = backGround.getGraphics();
       
        g.setColor(Color.GRAY);//灰色の背景
        g.fillRect(0, 0, this.getWidth(), this.getHeight());
       
        g.setColor(new Color(200, 100, 50).darker());//茶色の枠
        g.fill3DRect((int)(tableX[0]), (int)(tableY[0]), (int)(tableX[1]-tableX[0]), (int)(tableY[1]-tableY[0]), true);
       
        g.setColor(Color.GREEN);//緑の台
        g.fill3DRect((int)(holesX[0]), (int)(holesY[0]), (int)(holesX[2]-holesX[0]), (int)(holesY[1]-holesY[0]), false);
        g.setColor(Color.GREEN.brighter());
        g.drawLine((int)(1.0 *(holesX[2]-holesX[0]) / 3.0), (int)holesY[0], (int)(1.0 *(holesX[2]-holesX[0]) / 3.0), (int)holesY[1]);
        g.fillOval((int)(1.0 *(holesX[2]-holesX[0]) / 3.0)-2, (int)((holesY[1]+holesY[0])/2)-2, 4, 4);
        g.drawArc((int)(1.0 *(holesX[2]-holesX[0]) / 3.0)-20, (int)((holesY[1]+holesY[0])/2)-20, 40, 40, 90, 180);
       
        g.setColor(Color.DARK_GRAY);//穴
        double radio = hR-2;
        for (int iX = 0; iX<3; ++iX) {
            for (int iY = 0; iY<2; ++iY) {
                g.fillOval((int)(holesX[iX]-radio), (int)(holesY[iY]-radio), (int)(2*radio), (int)(2*radio));
            }
        }
        
        g.setColor(Color.BLACK);//穴
        for (int iX = 0; iX<3; ++iX) {
            for (int iY = 0; iY<2; ++iY) {
                g.fillOval((int)(holesX[iX]-radio), (int)(holesY[iY]-radio), (int)(2*radio-3), (int)(2*radio-3));
            }
        }
    }
    
    public void setSound(String audioname){
    	try{
    	audioFile = new File(audioname);
        format = AudioSystem.getAudioFileFormat(audioFile).getFormat();
        info = new DataLine.Info(Clip.class, format);
        line = (Clip)AudioSystem.getLine(info);
        line.open(AudioSystem.getAudioInputStream(audioFile));
        line.start();
    	 }
        catch(Exception e){
            e.printStackTrace();
        }
    }
   
   
    public void start() {
        (new Thread(this)).start();
    }
   
   
    public void run() {
       
        long t1 = System.currentTimeMillis(), t2 = t1;
       
        while (true) {
           
            try {
                               
                t2 = System.currentTimeMillis();
               
                switch (state) {
               
                    case WAITING_TO_HIT:
                        calculateNext(t2-t1);                
                        collisions();
                        update();
                       
                        break;
                       
                    case MOVING:
                        calculateNext(t2-t1);                
                        collisions();
                        update();
                       
                        boolean allStopped = true;//ボール停止したかの判定開始
                        for (int i=0; allStopped && i<nballs; ++i) {//1個でも動いてるのがあるか、台の上のボールを全部チェックしたら終了
                            allStopped = (vx[i]==0) && (vy[i]==0);
                        }
                        if (allStopped) {//全てのボールが止まったら
                            state = WAITING_TO_HIT;
                            if (!onTable[0]) {
                                setWhiteBall();
                            }
                        }
                       
                        //全部落としてクリアしたか、ゲームオーバー(分岐したい)
                        if (gameover) {
                        	state = FINISHING;
                        	compmessage = false;
                        	boomessage = true;
                        	gameover = false;
                        }
                        if (nBallsOn==0) {
                        	state = FINISHING;
                        	boomessage = false;
                        	compmessage = true;
                        }
                       
                        break;
                       
                    case FINISHING:
                        setBalls();  
                        setWhiteBall();
                        state = WAITING_TO_START;
                       
                        break;
                }
               
                render();
                repaint();
                t1 = t2;
               
                Thread.sleep(10);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
   
   
    public void collisions() {
        borderCollision();
        holesCollision();
        ballsCollision();
    }
   
   
    public void calculateNext(long millis) {
       
        double segs = millis / 1000.0;
       
        for (int i=0; i<nballs; ++i) if (onTable[i]){
            nextX[i] = x[i] + vx[i] * segs;
            nextY[i] = y[i] + vy[i] * segs;            
           
            vx[i] *= 0.99;//ボールの速度を段々減らす
            vy[i] *= 0.99;
           
            if (Math.abs(Math.hypot(vx[i], vy[i]))<2) {//hypot = 中間のオーバーフローやアンダーフローのない sqrt(x2 +y2)
                vx[i] = 0;
                vy[i] = 0;
            }
        }
    }
   
   
    public void holesCollision() {//穴に入った(衝突した)判定
       
        for (int ball = 0; ball < nballs; ++ball) /*←すべてのボールについて*/if (onTable[ball]) {
            for (int iHX=0; iHX<3; ++iHX) {//すべての穴について
                for (int iHY=0; iHY<2; ++iHY) {
                    if (Math.hypot(holesX[iHX]-x[ball], holesY[iHY]-y[ball])<hR) {//穴に接触したら
                    	
                    	
                        onTable[ball] = false;//落下したフラグ
                        if (ball!=0)--nBallsOn;//ボールの残り数を減らす
                        vx[ball] = 0;//落下したボールは動かない
                        vy[ball] = 0;
                        Desktop desktop = Desktop.getDesktop();
                        //動画にとぶよ〜！                     
                        try {
                        	if(ball!=0 && ball!=6 && ball!= 7 && ball!=8){
                            URI uri = new URI(uriString[ball-1]);
                            desktop.browse(uri);
                        	}
                            } catch (URISyntaxException e) {
                            e.printStackTrace();
                            } catch (IOException e) {
                            e.printStackTrace();
                            }
                        
                        if(order){//落とす順番を考慮する場合
                            if(!(ball==current+1) && 0 < ball){//白玉以外が間違った順番で落ちたら
                            	gameover = true;
                            }else{
                            	current++;
                            }
                        }
                        
                        if(ball == 6) indivimes = 1;
                        if(ball == 7) indivimes = 2;
                        if(ball== 8) indivimes = 3;
                        if(retry){//残機制限ありの場合
                        	if(ball==0){
                        		retrynum--;
                            	if(retrynum < 0){
                               		gameover = true;
                            	}
                        	}
                        }
                        setSound(pocket1);
                    }
                }
            }
        }        
    }
   
   
    public void ballsCollision() {
        for (int ball1=0; ball1<nballs; ++ball1) if (onTable[ball1]){
            for (int ball2=ball1+1; ball2<nballs; ++ball2) if (onTable[ball2]){
                boolean collision;
                if(collision = isBallsCollision(ball1,ball2)){//ボールが衝突してたら
                	
                	if(breakshot){//最初の衝突だったら
                		//billiard-break1.mp3...ブレイク
                		breakshot=false;
                	}else{
                		//billiard-ball1.mp3...球同士が衝突
                	}
                	
                    // Adjust position
                    int cont = 0;
                    while (cont <10 && collision){
                        nextX[ball1] = (nextX[ball1] + x[ball1]) / 2;
                        nextY[ball1] = (nextY[ball1] + y[ball1]) / 2;
                       
                        nextX[ball2] = (nextX[ball2] + x[ball2]) / 2;
                        nextY[ball2] = (nextY[ball2] + y[ball2]) / 2;
                       
                        collision = isBallsCollision(ball1, ball2);
                       
                        ++cont;
                    }
                   
                    if (collision) {
                        nextX[ball1] = x[ball1];
                        nextY[ball1] = y[ball1];
                       
                        nextX[ball2] = x[ball2];
                        nextY[ball2] = y[ball2];
                    }
 
                    // Adjust velocities
                    double dx = nextX[ball2] - nextX[ball1];
                    double dy = nextY[ball2] - nextY[ball1];
                    double dist = Math.hypot(nextX[ball1]-nextX[ball2], nextY[ball1]-nextY[ball2]);
                   
                    // cos(ang) = dx / dist
                    // sin(ang) = dy / dist;
                    // tg(ang) = dy / dx = sin(ang) / cos(ang)
                    double cos = dx/dist;
                    double sin = dy/dist;
                   
                    nextVx[ball2] = vx[ball2] - vx[ball2] * cos * cos;
                    nextVx[ball2] -= vy[ball2] * cos * sin;
                    nextVx[ball2] += vx[ball1] * cos * cos;
                    nextVx[ball2] += vy[ball1] * cos * sin;      
                   
                    nextVy[ball2] = vy[ball2] - vy[ball2] * sin * sin;
                    nextVy[ball2] -= vx[ball2] * cos * sin;
                    nextVy[ball2] += vx[ball1] * cos * sin;
                    nextVy[ball2] += vy[ball1] * sin * sin;
                   
                    nextVx[ball1] = vx[ball1] - vx[ball1] * cos * cos;
                    nextVx[ball1] -= vy[ball1] * cos * sin;
                    nextVx[ball1] += vx[ball2] * cos * cos;
                    nextVx[ball1] += vy[ball2] * cos * sin;        
                   
                    nextVy[ball1] = vy[ball1] - vy[ball1] * sin * sin;
                    nextVy[ball1] -= vx[ball1] * cos * sin;
                    nextVy[ball1] += vx[ball2] * cos * sin;
                    nextVy[ball1] += vy[ball2] * sin * sin;
                   
                    vx[ball1] = nextVx[ball1];
                    vy[ball1] = nextVy[ball1];
                   
                    vx[ball2] = nextVx[ball2];
                    vy[ball2] = nextVy[ball2];
                    //音だよん〜
                    setSound(bill1);
                    
                }
            }   
        }
    }
   
    public boolean isBallsCollisionPre(int ball1, int ball2) {
        return Math.hypot(x[ball1]-x[ball2], y[ball1]-y[ball2]) < 2*r;
    }
   
    public boolean isBallsCollision(int ball1, int ball2) {
        return Math.hypot(nextX[ball1]-nextX[ball2], nextY[ball1]-nextY[ball2]) < 2*r;
    }
   
    public void update() {//台の上にあるボールの座標を更新   
        for (int i=0; i<nballs; ++i) if(onTable[i]){
            x[i] = nextX[i];
            y[i] = nextY[i];        
        }
    }
   
   
    public void borderCollision() {//壁との衝突判定
               
        for (int i=0; i<nballs; ++i) if (onTable[i]) {
           
            if (nextX[i]-r<holesX[0]) {
                nextX[i] = holesX[0] + r;
                vx[i] *= -1;
            }
            else if (nextX[i]+r>holesX[2]){
                nextX[i] = holesX[2]-r;
                vx[i] *= -1;
            }
           
            if (nextY[i]-r<holesY[0]) {
                nextY[i] = holesY[0] + r;
                vy[i] *= -1;
            }
            else if (nextY[i]+r>holesY[1]) {
                nextY[i] = holesY[1] - r;
                vy[i] *= -1;
            }
        }
    }
     
   
    public void render() {      
 
        Graphics gBackBuffer = backBuffer.getGraphics();
        gBackBuffer.setFont(new Font("Courier", Font.BOLD, 20));
       
       
        // TABLE
        gBackBuffer.drawImage(backGround, 0, 0, null);          
       
        //スコア表示
        gBackBuffer.setColor(Color.WHITE);
        gBackBuffer.drawString("score " + clicktimes, 20, 20);
        
        //残機表示
        if(retry){
        	for(int i = 0; i < retrynum; i++){
                gBackBuffer.setColor(Color.WHITE);
                gBackBuffer.fillOval((int)(20 + 20*i), 330, (int)(r*2), (int)(r*2));
            }
        }
        
        // BALLS
        //白いボール
        if (onTable[0]) {
            gBackBuffer.setColor(Color.WHITE);
            gBackBuffer.fillOval((int)(x[0]-r), (int)(y[0]-r), (int)(r*2), (int)(r*2));
        }
       
        //gBackBuffer.setColor(Color.RED);//デフォルトは全部赤
        //落とすボール
        for (int i=1; i<nballs; i++) if (onTable[i]){
        	gBackBuffer.setColor(new Color(ballcolors[i%9][0], ballcolors[i%9][1], ballcolors[i%9][2]));
            gBackBuffer.fillOval((int)(x[i]-r), (int)(y[i]-r), (int)(r*2), (int)(r*2));
            gBackBuffer.drawString(String.valueOf(Math.max((i+1)%10, 1)), (int)(x[i]-r), (int)(y[i]-r));//0716修正
        }
       
        gBackBuffer.setColor(Color.BLACK);
        for (int i=0; i<nballs; ++i) if (onTable[i]) {
            gBackBuffer.drawOval((int)(x[i]-r), (int)(y[i]-r), (int)(r*2), (int)(r*2));
        }
       
        // STICK
        if (state == WAITING_TO_HIT) drawStick(gBackBuffer);
        
        //メッセージ表示
        if(indivimes == 1)           gBackBuffer.drawString("みやちゃんめっせげっと〜", 400,300 );
        if(indivimes == 2)           gBackBuffer.drawString("さっちゃんめっせげっと〜", 400,300 );
        if(indivimes == 3)           gBackBuffer.drawString("えりかめっせげっと〜", 400,300 );
        // Initial message
        if (state == WAITING_TO_START) {            
            int mX = this.getWidth()/2-85;
            int mY = this.getHeight()/2;
                       
            gBackBuffer.setColor(Color.BLACK);
            gBackBuffer.drawString("Click to start", mX+2, mY+2);
            gBackBuffer.drawString("まささんソースコード見ちゃダメですよ(癶u癶)??", mX-128, mY+52);
           
            if (((System.currentTimeMillis()/1000)&1)==0) {
            	gBackBuffer.setColor(Color.YELLOW);
            }
            else {
                gBackBuffer.setColor(Color.CYAN);
            }
            gBackBuffer.drawString("Click to start", mX, mY);
            gBackBuffer.drawString("まささんソースコード見ちゃダメですよ(癶u癶)??", mX-130, mY+52);
            
            if (((System.currentTimeMillis()/1000)&1)==0) {
            	if(boomessage){
            		gBackBuffer.setColor(Color.RED);
            		gBackBuffer.setFont(new Font("MSゴシック", Font.BOLD, 30));
            		gBackBuffer.drawString("まささん Game Over♥ (⌒◯⌒)/", mX-140, mY-145);	
            	}
            	/*if (compmessage){
            		gBackBuffer.setColor(Color.PINK);
            		gBackBuffer.setFont(new Font("MSゴシック", Font.BOLD, 20));
            		gBackBuffer.drawString("まささん Game Completeですよ♥ (♥v♥)/", mX-140, mY-150);
            		gBackBuffer.setColor(Color.PINK);
            		gBackBuffer.setFont(new Font("MSゴシック", Font.BOLD, 30));
            		gBackBuffer.drawString("??One More Time??(♥v♥)", mX-120, mY-50);
            		
            	}*/
            }
            
            if (compmessage){
        		gBackBuffer.setColor(Color.PINK);
        		gBackBuffer.setFont(new Font("MSゴシック", Font.BOLD, 20));
        		gBackBuffer.drawString("まささん Game Completeですよ♥ (♥v♥)/", mX-120, mY-150);
        		gBackBuffer.setColor(Color.PINK);
        		gBackBuffer.setFont(new Font("MSゴシック", Font.BOLD, 30));
        		gBackBuffer.drawString("??One More Time??(♥v♥)", mX-115, mY-50);
        		
        	}
            
            //スコアなどのカウントを初期値に戻す
            retrynum = RETRYNUM;
            clicktimes = 0;
            current = 0;
        }
      
    }
   
   
    public void drawStick(Graphics gBackBuffer) {
           
        double dist = Math.hypot(x[0]-mX, y[0]-mY);
        double dXNormalized = (mX-x[0])/dist;
        double dYNormalized = (mY-y[0])/dist;
        double strength = (clicked) ? strength()/10 : 1;
        double x1 = x[0] + dXNormalized * (r+strength);
        double x2 = x[0] + dXNormalized * (r+sL+strength);
        double y1 = y[0] + dYNormalized * (r+strength);
        double y2 = y[0] + dYNormalized * (r+sL+strength);
       
       
        // Draw stick
        gBackBuffer.setColor(Color.ORANGE);
        gBackBuffer.drawLine((int)x1, (int)y1, (int)x2, (int)y2);
   
        // Draw path line
        int dot = 0;
        double d = Math.hypot(x[0]-mXPost,y[0]-mYPost);//白玉とカーソルの距離
        int nDots = (clicked)//長さの制限？
            ? (int)(150.0 * (strength / MAX_STRENGTH))
            : (int)(1 + Math.max(0.4 * d, 0));//強く打つときほどガイドラインを長くする
        double step = 30;
        double xStep = step * dXNormalized;
        double yStep = step * dYNormalized;        
       
        double nextX = x[0] + actualStep * dXNormalized;
        double nextY = y[0] + actualStep * dYNormalized;
        --actualStep;
        actualStep %= step;
       
        //gBackBuffer.setColor(Color.WHITE);//点線の色を白で固定する場合
        gBackBuffer.setColor(new Color((float)Math.min(d*0.005f, 1),  (float)Math.min(d*0.005f, 1), 1.0f));//強さによって点線の色を変える場合
       
        boolean hit=false;//ガイドラインが台の上の球に重なっているか
        for (; dot<nDots; ++dot) {
        	//左右の壁にぶつかりそうな場合
            if (nextX < holesX[0]) {
                nextX = holesX[0] - nextX;
                nextX = holesX[0] + nextX;
                xStep *= -1;
            }
            else if (nextX > holesX[2]) {
                nextX = nextX - holesX[2];
                nextX = holesX[2]-nextX;
                xStep *= -1;
            }
           
            //上下の壁にぶつかりそうな場合
            if (nextY < holesY[0]) {
                nextY = holesY[0]-nextY;
                nextY = holesY[0]+nextY;
                yStep *= -1;
            }
            else if(nextY > holesY[1]) {
                nextY = nextY - holesY[1];
                nextY = holesY[1] - nextY;
                yStep *=-1;
            }
           
            hit = (checkhit(nextX-2, nextY-2) || hit);//ガイドラインが球に当たるか
           
            if(!hit){
            	gBackBuffer.fillOval((int)nextX-2, (int)nextY-2, 4, 4);//球にぶつからなければ点線描写
            }
            	
            nextX -= xStep;
            nextY -= yStep;
        }
    }
   
    public boolean checkhit(double lx, double ly){//ガイドラインが狙う玉と重なっているか判定
    	boolean hit = false;
    	for (int i=1; i<nballs; ++i) if (onTable[i]) {//台の上にあるすべての球について
    		if( Math.hypot(x[i]-lx , y[i]-ly) < (int)(r*2.3)){//ガイドラインと球の座標の距離が一定値以下だったら
    			hit = true;//当たりと判定
    			break;
    		}
    	}
    	return hit;
    }
   
    public double strength() {
        //return Math.abs(mYPost-mY);
        if (clicked) {
            return Math.min(MAX_STRENGTH, 10 * Math.hypot(x[0]-mXPost,y[0]-mYPost));
        }
        else {
            return Math.min(MAX_STRENGTH, 10 * Math.hypot(mX-mXPost, mY-mYPost));
        }
    }
   
    public void paint(Graphics g) {
        g.drawImage(backBuffer, 0, 0, null);
    }
       
   
    // MOUSE LISTENER METHODS
   
    public void mousePressed(MouseEvent e) {
        clicked = true;
        
    }
   
    public void mouseReleased(MouseEvent e) {
       
       
        if (state==WAITING_TO_HIT) {
            double dStickBall = Math.hypot(x[0]-mX, y[0]-mY);
            double dXNormalized = (x[0]-mX)/dStickBall;
            double dYNormalized = (y[0]-mY)/dStickBall;
            double strength = strength();
           
            if (strength>0) {
                state = MOVING;
                //System.out.println("strength>0; state set moving");
                vx[0] = strength * dXNormalized;
                vy[0] = strength * dYNormalized;
            }
            
            setSound(shot1);
            
            //以下追記
            clicktimes++;
            //System.out.println("clicked");
            
        }
       
        clicked = false;
    }
   
    public void mouseClicked(MouseEvent e) {
        if (state == WAITING_TO_START) {
        	//billiard-shot1.mp3...ショット（球にキューを当てる）
            state = WAITING_TO_HIT;
        }
    }
   
    public void mouseEntered(MouseEvent e) {
        // EMPTY
    }
   
    public void mouseExited(MouseEvent e) {
        // EMPTY
    }
   
   
    // MOUSEMOTIONLISTENER METHODS
   
    public void mouseMoved(MouseEvent e) {        
        mXPost = e.getX();
        mYPost = e.getY();
        mX = mXPost;
        mY = mYPost;
    }
   
    public void mouseDragged(MouseEvent e) {
        mXPost = e.getX();
        mYPost = e.getY();
    }    
   
   
    public static void main(String[] args) {
        new Billard4K();
    }
}