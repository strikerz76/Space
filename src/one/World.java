package one;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

public class World implements ActionListener {
  
  public static final boolean GIVE_LOTS_OF_MONEY_FOR_TESTING = true;
  
  public static final boolean BUILD_WALL_MODE_ENABLED = true;
  
  public static final boolean METEOR_GROWS_FAST = false;
  
	private ArrayList<Ship> ships;
	private ArrayList<Base> bases;
	private ArrayList<Laser> lasers;
//  private ArrayList<Laser> lasersToSend;
  private ArrayList<Object> sendBuffer;
	private ArrayList<Wall> walls;
	private ArrayList<String> messages;
	ArrayList<Color> available;
	ArrayList<Color> disconnected;
	
	public Meteor met;
	Timer t;
	int con = 0;
	boolean gamestarted = false;
	boolean superenabled = true;
	boolean meteorenabled = true;
  boolean pitypoints = true;
	int timeformeteor = 0;
	final int METEORCD = 200;
	Server server;
	ArrayList<Connection> connections;
	
	public int WORLDX;
	public int WORLDY;
	ArrayList<Integer> WORLDINIT;
	ArrayList<Integer> baseattacked;
	
	MFrame mframe;
	TFrame console;
	BFrame options;
	
	boolean gamepaused;
	boolean randomspawn;
	final int startingpoints;
	int cdtogivemoney;
	public World(Server s) {
	  sendBuffer = new ArrayList<Object>();
		baseattacked = new ArrayList<Integer>();
		baseattacked.add(Client.BASEATTACKED);
		baseattacked.add(1);
		WORLDINIT = new ArrayList<Integer>();
		WORLDINIT.add(Client.WORLDSIZE);
		WORLDINIT.add(1);
		WORLDINIT.add(0);
		WORLDINIT.add(0);
		WORLDINIT.add(1);
		server = s;
		
		options = new BFrame();
		mframe = new MFrame();
		console = new TFrame();
		
		mframe.setTitle("Space Spectating Client");
		console.setTitle("Space Console  (Creating Server)");
		options.setTitle("Space Server Creator");
		
		ImageIcon ii = new ImageIcon("resources/images/icon.png");// setting the space icon
		Image icon = ii.getImage();
		mframe.setIconImage(icon);
		console.setIconImage(icon);
		options.setIconImage(icon);
		
		startingpoints = (int)(Math.random()*20+10);
		connections = new ArrayList<Connection>();
		ships = new ArrayList<Ship>();
		bases = new ArrayList<Base>();
		lasers = new ArrayList<Laser>();
		walls  = new ArrayList<Wall>();
		messages = new ArrayList<String>();
		available = new ArrayList<Color>();
		disconnected = new ArrayList<Color>();
		available.add(Color.blue);
    available.add(new Color(0, 220, 220)); // cyan
		available.add(Color.red);
		available.add(new Color(0, 240, 0)); // green
    available.add(new Color(150, 150, 255)); // blueish
    available.add(new Color(200, 230, 100)); // yellow
		available.add(Color.MAGENTA);
		available.add(new Color(240, 120, 0)); // orange
		available.add(Color.pink);
		available.add(Color.GRAY);
    available.add(new Color( 150, 0, 200)); // dark purple
    available.add(new Color( 0, 150, 0)); // dark green
    available.add(new Color( 160, 82, 45)); // brownish
		met = new Meteor(0, 0, 0);
		ii = new ImageIcon("resources/images/meteor.png");
		Meteor.image = ii.getImage();
		Base.world = this;
		Ship.world = this;
		Laser.world = this;
//		server = new Server(this);
//		server.start();
		String newTitle = "Space Console  (Server Created)";
		try {
		  newTitle += "ip= " + InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e1) {
      e1.printStackTrace();
    }
		console.setTitle(newTitle);
		
		t = new Timer(100, this);
		t.start();
	}
	public void setSizeOfWorld(int x, int y) {
		WORLDX = x;
		WORLDY = y;
		Meteor.startingWidth = (x+y)/36;
	}
	/**
	 * sends current worldsize and superenabled to all clients
	 */
	public void updateWorld() {
	  WorldInfo wi = new WorldInfo(this.WORLDX, this.WORLDY, this.superenabled);
    for(int a=connections.size()-1; a>=0; a--) {
      connections.get(a).send(wi);
    }
//		WORLDINIT.set(2, WORLDX);
//		WORLDINIT.set(3, WORLDY);
//		WORLDINIT.set(4, Client.TRUE);
//		if(!superenabled) {
//			WORLDINIT.set(4, Client.FALSE);
//		}
//		for(int a=connections.size()-1; a>=0; a--) {
//			connections.get(a).send(WORLDINIT);
//		}
	}
	public boolean canJoin() {
		if(!hasSpace())
			return false;
		if(gamestarted()) {
			if(disconnected.size()<=0)
				return false;
		}
		return true;
	}
	public boolean hasSpace() {
		if(randomspawn) {
		} else {
			if(bases.size()>=8) {
				return false;
			}
		}
		return true;
	}
	public boolean gamestarted() {
		return gamestarted;
	}
	public boolean hasdisconnect() {
		return (disconnected.size()>0);
	}
	public void removeConnection(one.Connection connection) {
		if(connections.remove(connection)) {
			playerDisconnected(connection.player);
		}
	}
	public boolean tooclose(int x, int y) {
		for(Base b : bases) {
			int dist = Math.abs(b.cur.x-x)+Math.abs(b.cur.y-y);
			if(dist<=400)
				return true;
		}
		return false;
	}
	public boolean isColorGood(Color c) {
		int total = c.getBlue()+c.getRed()+c.getGreen();
		int bl = c.getBlue();
		int re = c.getRed();
		int gr = c.getGreen();
		int numUnder100 = 0;
		if( re < 100 ) numUnder100++;
		if( gr < 100 ) numUnder100++;
    if( bl < 100 ) numUnder100++;
    if( numUnder100 == 2 ) {
      if( total < 150 ) {
        return false;
      }
    }
		if( c.getBlue() < 110 && c.getGreen() < 110 && c.getRed() < 110 ) {
		  return false;
		}
		if(total<100 || total>690)
			return false;
		int MIN = 150;
		for(Base b : bases) {
			Color p = b.player;
			if(Math.abs(p.getRed()-c.getRed())+Math.abs(p.getGreen()-c.getGreen())+Math.abs(p.getBlue()-c.getBlue())<MIN) {
				return true;
			}
		}
		return false;
	}
	public Color getRandomColor() {
	  return new Color((int)(Math.random()*175)+80, (int)(Math.random()*175)+80, (int)(Math.random()*175)+80);
	}
	public Color selectColor() {
	  Color co = Color.black;
	  if( Math.random() < .9 && available.size() > 0) {
      for( int i = 0; i < 10 && available.size() > 0 ; i++ ) {
        co = available.remove((int)(Math.random() * available.size()));
        if( isColorGood(co) ) {
          break;
        }
      }
    } 
    if( !isColorGood(co) ) {
      do {
        co = getRandomColor();
      } while(isColorGood(co));
    }
    return co;
	}
	
	public void addConnection(Connection connection) {
		connections.add(connection);
		connection.start();
		System.out.println("Adding connection");
		Color co;
		if(disconnected.size()>0)
			co = disconnected.remove(0);
		else {
		  co = selectColor();
		}
		Base b = this.getbase(co);
		if(b==null) {
			if(randomspawn) {
				int x=0;
				int y=0;
				int w=(int)(Math.random()*10+40);
				do {
					x = (int)(Math.random()*(WORLDX-100)+50);
					y = (int)(Math.random()*(WORLDY-100)+50);
				} while (tooclose(x, y));
				b = new Base(co, x, y, w, startingpoints);
			} else {
				if(con==0) {
					b = new Base(co, 110, 110, 40, startingpoints);
//					walls.add(new Wall(350, 0, 10, 150));
				}
				if(con==1) {
					b = new Base(co, WORLDX-150, 110, 40, startingpoints);
	//				walls.add(new Wall(750, 250, 250, 10));
				}
				if(con==2) {
					b = new Base(co, WORLDX-150, WORLDY-150, 40, startingpoints);
	//				walls.add(new Wall(650, 550, 10, 150));
				}
				if(con==3) {
					b = new Base(co, 110, WORLDY-150, 40, startingpoints);
	//				walls.add(new Wall(0, 500, 150, 10));
				}
				if(con==4) {
					b = new Base(co, WORLDX/2-20, 60, 40, startingpoints);
				}
				if(con==5) {
					b = new Base(co, 60, WORLDY/2-20, 40, startingpoints);
				}
				if(con==6) {
					b = new Base(co, WORLDX-100, WORLDY/2-20, 40, startingpoints);
				}
				if(con==7){
					b = new Base(co, WORLDX/2-20, WORLDY-100, 40, startingpoints);
				}
				if(con>7){
					return;
				}
			}
      walls.add(new Wall(WORLDX/3-20, 0, 20, WORLDY/5));
      walls.add(new Wall(WORLDX*2/3, 0, 20, WORLDY/5));
      walls.add(new Wall(0, WORLDY/3, WORLDX/5, 20));
      walls.add(new Wall(0, WORLDY*2/3-20, WORLDX/5, 20));
      walls.add(new Wall(WORLDX-WORLDX/5, WORLDY/3, WORLDX/5, 20));
      walls.add(new Wall(WORLDX-WORLDX/5, WORLDY*2/3-20, WORLDX/5, 20));
			bases.add(b);
			con++;
//			}
		}
		connection.setPlayer(b.player);
		updateWorld();
	}
	public void activateInvis(Color player) {
	  for( Ship ship : ships ) {
	    if( ship.player.equals(player)) {
	      ship.setInvisible(true);
//	      System.out.println("Ship is becoming invisible");
	    }
	  }
	}
	public void disableInvis(Color player) {
    for( Ship ship : ships ) {
      if( ship.player.equals(player)) {
        ship.setInvisible(false);
//        System.out.println("Ship is becoming visible");
      }
    }
	}
	public boolean collides(Laser l) {
		for(Wall w : walls) {
			Rectangle r = new Rectangle(w.x, w.y, w.w, w.h);
			if(r.intersectsLine(l.cur.x, l.cur.y, l.tarx(), l.tary())) {
				return true;
			}
		}
		return false;
	}
	public boolean collides(Rectangle r, Ship ship) {
		for(Ship s : ships) {
			if(s!=ship && s.collides(r)) {
				return true;
			}
		}
		for(Base b : bases) {
			if(b.collides(r)) {
				return true;
			}
		}
		for(Wall w : walls) {
			if(w.collides(r)) {
				return true;
			}
		}
		if(r.x<0 || r.x+r.width>WORLDX || r.y<0 ||r.y+r.height>WORLDY) {
			return true;
		}
		return false;
	}
	public boolean collides(Rectangle r) {
		for(Ship s : ships) {
			if(s.collides(r)) {
				return true;
			}
		}
		for(Base b : bases) {
			if(b.collides(r)) {
				return true;
			}
		}
		if(r.x<0 || r.x+r.width>WORLDX || r.y<0 ||r.y+r.height>WORLDY) {
			return true;
		}
		return false;
	}
	public boolean wallcollides(Rectangle r) {
//		System.out.println("checking collide");
		for(Ship s : ships) {
			if(s.collides(r)) {
				return true;
			}
		}
		for(Base b : bases) {
			if(b.collides(r)) {
				return true;
			}
		}
//		System.out.println("walldidnt collide");
		return false;
	}
	public void playerDisconnected(Color c) {
		System.out.println("player "+c+" disconnected");
		for(int a=0; a<bases.size(); a++) {
			Base b = bases.get(a);
			if(b.player.equals(c)) {
				disconnected.add(0, c);
				return;
			}
		}
	}
	public void removeBase(Base b) {
		bases.remove(b);
		b.removeThis = true;
		sendBuffer.add(b);
		/*
		ArrayList<Integer> i = new ArrayList<Integer>();
		i.add(Client.REMOVEBASE);
		i.add(1);
		i.addAll(b.convert());
		for(int a=0; a<connections.size(); a++) {
			Connection c = connections.get(a);
			c.send(i);
		}
		*/
	}
	public void removeShip(Ship s) {
		for(Base b : bases) {
			if(b.player.equals(s.player)) {
				b.numships--;
			}
		}
		ships.remove(s);
		s.removeThis = true;
		sendBuffer.add(s);
//		ArrayList<Integer> i = new ArrayList<Integer>();
//		i.add(Client.REMOVESHIP);
//		i.add(1);
//		i.addAll(s.convert());
//		for(int a=0; a<connections.size(); a++) {
//			Connection c = connections.get(a);
//			c.send(i);
//		}
	}
	public void tic() {
		if(gamestarted && !gamepaused) {
			if(pitypoints && cdtogivemoney++>15 && bases.size() > 0 ) {
				Base least = bases.get(0);
				for(int a=1; a<bases.size(); a++) {
					if(bases.get(a).totalworth<least.totalworth) {
						least = bases.get(a);
					}
				}
				if( GIVE_LOTS_OF_MONEY_FOR_TESTING ) {
  				for( int a = 0; a < 40; a++ )
  				  gotshipkill(least.player);
				}
        gotshipkill(least.player);
				cdtogivemoney=0;
			}
			for(int a=lasers.size()-1; a>=0; a--) {
				Laser l = lasers.get(a);
				l.tic();
				if(l.hit) 
					lasers.remove(l);
			}
			for(int a=bases.size()-1; a>=0; a--) {
				Base b = bases.get(a);
				b.tic();
			}

      long startTime = System.currentTimeMillis();
			for(int a=ships.size()-1; a>=0; a--) {
				Ship s = ships.get(a);
				s.tic();
			}
      long endTime = System.currentTimeMillis();
      Ship.collisionTime += endTime - startTime;
      //System.err.println("Collision Calls: " + Ship.collisionCalls + "\t\tCollision Time: " + Ship.collisionTime);
      Ship.collisionCalls = 0;
      Ship.collisionTime = 0;
			if(meteorenabled) {
				if(timeformeteor++>METEORCD) {
					if((met!=null && met.disabled()) || met==null) {
						met = new Meteor(new Point((int)(Math.random()*1200-100), (int)(Math.random()*1000-100)), (int)(Math.random()*10+15), new Point((int)(Math.random()*1200-100), (int)(Math.random()*1000-100)), WORLDX, WORLDY);
						timeformeteor=0;
					}
				}
				met.tic();
			}
		}
    long startTime = System.currentTimeMillis();
		ArrayList<Integer> bases, meteors, walls;
		//bases = convertbases();
//		meteors = convertMeteor();
//		walls = convertwalls();
		Object[] listships = this.ships.toArray();
		Object[] listbases = this.bases.toArray();
		Object[] listlasers = this.sendBuffer.toArray();
		this.sendBuffer.clear();
		Object[] listmeteors = {};
		if( this.met != null ) {
	    listmeteors = new Object[1];
	    listmeteors[0] = this.met;
		}
		Object[] listwalls = this.walls.toArray();
		Object[][] arrays = {listships, listbases, listlasers, listmeteors, listwalls};
		Object[] list = combineArrays(arrays);
		for(int a=0; a<connections.size(); a++) {
			Connection c = connections.get(a);
			c.send(list);
			//c.send(listbases);
			c.reset();
//			c.send(bases);
//			sendStats(c);
//			c.send(meteors);
//			c.send(walls);
		}
    long endTime = System.currentTimeMillis();
    long sendTime = endTime - startTime;
    //System.err.println("sendTime: " + Ship.collisionTime);
	}
	public Object[] combineArrays( Object[][] arrays ) {
	  int size = 0;
	  for( int index = 0; index < arrays.length; index++ ) {
	    size += arrays[index].length;
	  }
	  Object[] array = new Object[size];
	  int index = 0;
	  for( int i = 0; i < arrays.length; i++ ) {
	    for( int j = 0; j < arrays[i].length; j++, index++ ) {
	      array[index] = arrays[i][j];
	    }
	  }
	  return array;
	}
	public void pause() {
		ArrayList<Integer> send = new ArrayList<Integer>();
		send.add(Client.PAUSE);
		send.add(1);
		if(gamepaused) {
			System.out.println("pausing");
			send.add(1);
		} else {
			System.out.println("unpausing");
			send.add(0);
		}
		for(int a=0; a<connections.size(); a++) {
			Connection c = connections.get(a);
			c.send(send);
		}
	}
	public void addLaser(Laser l) {
		lasers.add(l);
		sendlaser(l);
	}
	public void sendlaser(Laser l) {
	  sendBuffer.add(l);
  }
//	public void sendlaser(Laser l) {
//		ArrayList<Integer> i = new ArrayList<Integer>();
//		i.add(Client.LASER);
//		i.add(1);
//		i.addAll(l.convert());
//		for(int a=0; a<connections.size(); a++) {
//			Connection c = connections.get(a);
//			c.send(i);
//		}
//	}
	public Base getbaseinrange(Ship ship) {
		for(Base s : bases) {
			if(!ship.player.equals(s.player) && ship.distance(s)<ship.RANGE) {
				return s;
			}
		}
		return null;
	}
	public Ship getinrange(Ship ship) {
		for(Ship s : ships) {
			if(!ship.player.equals(s.player) && !s.dead && s.distance(ship)<ship.RANGE) {
				return s;
			}
		}
		return null;
	}
	public Ship getinrange(Base ship) {
		for(Ship s : ships) {
			if(!ship.player.equals(s.player) && s.distance(ship)<ship.RANGE*2) {
				return s;
			}
		}
		return null;
	}
	public int getNumShips(Color c) {
		int num=0;
		for(Ship s : ships) {
			if(s.player.equals(c)) {
				num++;
			}
		}
		return num;
	}
	public boolean addShip(Ship s) {
		ships.add(s);
		return true;
	}
	public boolean addBase(Base b) {
		bases.add(b);
		return true;
	}
	public void sendStats(Connection c) {
		ArrayList<Integer> i = new ArrayList<Integer>();
		i.add(Client.STATS);
		i.add(1);
		Base b = this.getbase(c.player);
		if(b==null)
			return;
		i.addAll(b.convertstats());
		c.send(i);
	}
	public Base getbase(Color c) {
		for(Base b : bases) {
			if(b.player.equals(c))
				return b;
		}
		return null;
	}
	public void playerclicking(Color which, Point where) {
	  Point target = new Point(where);
		for(Ship s : ships) {
			if(s.player.equals(which)) {
				s.setTarget(target);
			}
		}
	}
	public void gotbasekill(Color c) {
		for(Base b : bases) {
			if(b.player.equals(c)) {
				b.addpoints(30);
			}
		}
	}
	public void gotshipkill(Color c) {
		for(Base b : bases) {
			if(b.player.equals(c)) {
				b.addpoints(1);
			}
		}
	}
	public void gotsuperkill(Color c) {
		for(Base b : bases) {
			if(b.player.equals(c)) {
				b.addpoints(3);
			}
		}
	}
	@Override
	public void actionPerformed(ActionEvent arg0) {
		tic();
		mframe.repaint();
	}
	public class MFrame extends JFrame implements MouseListener{
		Point pressed;
		public MFrame() {
			int width = Toolkit.getDefaultToolkit().getScreenSize().width;
			if(width>1000)
				width=1000;
			int height = Toolkit.getDefaultToolkit().getScreenSize().height;
			if(height>800)
				height=800;
			this.setSize(width, height);
			this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			this.setTitle("Space");
//	    	this.setLayout(null);
	        this.setLocationRelativeTo(null);
	        this.addMouseListener(this);
	        this.setResizable(true);
			this.setFocusable(true);
			Panel p = new Panel();
//			p.setBounds(0, 0, this.getWidth(), this.getHeight());
			this.add(p, BorderLayout.CENTER);
			this.validate();
			this.repaint();
		}
		@Override
		public void mouseClicked(MouseEvent arg0) {	
		}
		@Override
		public void mouseEntered(MouseEvent arg0) {	
		}
		@Override
		public void mouseExited(MouseEvent arg0) {	
		}
		@Override
		public void mousePressed(MouseEvent e) {
			pressed = e.getPoint();
		}
		@Override
		public void mouseReleased(MouseEvent e) {	
			if(met!=null && met.disabled())
				met = new Meteor(pressed, 10, e.getPoint(), WORLDX, WORLDY);
		}
	}
	Point lookingat = new Point(0, 0);
	public class Panel extends JPanel {
		@Override
		public void paintComponent(Graphics g) {
			BufferedImage image = new BufferedImage(WORLDX, WORLDY, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g2d = (Graphics2D)image.getGraphics();
			RenderingHints rh = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        	rh.put(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
        	g2d.setRenderingHints(rh);
			g2d.setColor(Color.black);
			g2d.fillRect(0, 0, WORLDX, WORLDY);
			if(met!=null)
				met.draw(g2d,lookingat);
			for(Ship s:ships) {
				s.draw(g2d, lookingat);
			}
			for(Base b : bases) {
				b.draw(g2d, lookingat);
			}
			for(Laser l : lasers) {
				l.draw(g2d, lookingat);
			}
			g.setFont(new Font("Arial", 50, 70));
			if(!gamestarted) {
				g.drawString("Waiting for server to start game", 50, getHeight()/2);
			}
			if(gamepaused) {
				g.drawString("GAME PAUSED", 50, getHeight()/2+50);
			}
			g.setColor(Color.white);
			g.drawRect(0-lookingat.x, 0-lookingat.y, WORLDX, WORLDY);
			g.drawImage(image, 0, 0, mframe.getWidth(), mframe.getHeight()+25, null);
		}
	}
	public class TFrame extends JFrame implements KeyListener, ActionListener{
		JButton start;
		JButton meteor;
		JButton supers;
		JButton pause;
		JButton random;
    JButton pity;
		public TFrame() {
			start = new JButton("Start Game");
			start.addActionListener(this);
			start.setVisible(true);
			start.setSize(100, 50);
			start.setLocation(10, 10);
			this.add(start);
			pause = new JButton("Pause");
			pause.addActionListener(this);
			pause.setVisible(true);
			pause.setSize(100, 50);
			pause.setLocation(120, 10);
//			this.add(pause);
			meteor = new JButton("Meteor On");
			meteor.addActionListener(this);
			meteor.setVisible(true);
			meteor.setSize(100, 50);
			meteor.setLocation(120, 10);
			this.add(meteor);
			supers = new JButton("Super On");
			supers.addActionListener(this);
			supers.setVisible(true);
			supers.setSize(100, 50);
			supers.setLocation(230, 10);
			this.add(supers);
			random = new JButton("RandomOff");
			random.setToolTipText("Randomly positioned spawns");
			random.addActionListener(this);
			random.setVisible(true);
			random.setSize(100, 50);
			random.setLocation(340, 10);
			this.add(random);
      pity = new JButton("Pity On");
      pity.addActionListener(this);
      pity.setVisible(true);
      pity.setSize(100, 50);
      pity.setLocation(450, 10);
      this.add(pity);
			this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			this.setTitle("Space Console");
	    	this.setLayout(null);
	    	this.setSize(780, 100);
	        this.setLocationRelativeTo(null);
	        this.setResizable(false);
			this.setFocusable(true);
			this.addKeyListener(this);
			this.validate();
			this.repaint();
	        this.setVisible(false);
		}
		@Override
		public void keyPressed(KeyEvent e) {
		}
		@Override
		public void keyReleased(KeyEvent e) {
			if(e.getKeyCode()==KeyEvent.VK_SPACE) {
				gamestarted = true;
				console.setTitle("Space Console (Game Started)");
			}
			if(e.getKeyCode()==KeyEvent.VK_BACK_SPACE) {
				System.exit(0);
			}
		}
		@Override
		public void keyTyped(KeyEvent e) {
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			if(e.getSource()==start) {
				this.remove(supers);
				this.remove(meteor);
				this.remove(start);
				this.remove(random);
        this.remove(pity);
				pause.setLocation(10, 10);
				this.add(pause);
				gamestarted = true;
				mframe.setVisible(true);
				console.setTitle("Space Console  (Game Started)");
        walls.add(new Wall(WORLDX/2 - WORLDX/7, WORLDY/2 - 10, WORLDX/14, 20));
        walls.add(new Wall(WORLDX/2 - 10, WORLDY/2 - WORLDY/7, 20, WORLDY/14));
        
        walls.add(new Wall(WORLDX/2 + WORLDX/14, WORLDY/2 - 10, WORLDX/14, 20));
        walls.add(new Wall(WORLDX/2 - 10, WORLDY/2 + WORLDY/14, 20, WORLDY/14));
        for( int a = 0; a < 20; a++ ) {
          walls.add(new Wall((int)(Math.random()*WORLDX), (int)(Math.random()*WORLDY), 20, 20));
        }
//				WORLDSIZE.set(2, WORLDX);
//				WORLDSIZE.set(3, WORLDY);
//				for(int a=connections.size()-1; a>=0; a--) {
//					for(Integer i : WORLDSIZE) {
//						System.out.println(i);
//					}
//					connections.get(a).send(WORLDSIZE);
//				}
			}
			if(e.getSource()==meteor) {
				meteorenabled=!meteorenabled;
				if(meteorenabled) {
					meteor.setText("Meteor On");
				} else {
					if(met!=null)
						met.disable();
					meteor.setText("Meteor Off");
				}
			}
			if(e.getSource()==supers) {
				superenabled=!superenabled;
				updateWorld();
				if(superenabled) {
					supers.setText("Super On");
				} else {
					supers.setText("Super Off");
				}
			}
			if(e.getSource()==pause) {
				gamepaused=!gamepaused;
				if(gamepaused) {
					pause.setText("Resume");
					console.setTitle("Space Console  (Game Paused)");
				} else {
					pause.setText("Pause");
					console.setTitle("Space Console  (Game Started)");
				}
				pause();	
			}
			if(e.getSource()==random) {
				randomspawn=!randomspawn;
				if(randomspawn) {
					random.setText("Random On");
				} else {
					random.setText("RandomOff");
				}
			}
			if( e.getSource() == pity ) {
			  pitypoints = !pitypoints;
			  if( pitypoints) {
			    pity.setText("Pity On");
			  }
			  else {
			    pity.setText("Pity Off");
			  }
			}
			this.repaint();
		}
	}
	public class BFrame extends JFrame implements KeyListener, ActionListener{
		JButton startserver;
		JButton small;
		JButton medium;
		JButton large;
		JButton selected;
		public BFrame() {
			startserver = new JButton("Create");
			startserver.addActionListener(this);
			startserver.setVisible(true);
			startserver.setSize(100, 50);
			startserver.setLocation(10, 10);
			this.add(startserver);
			
			medium = new JButton("Medium");
			medium.addActionListener(this);
			medium.setVisible(true);
			medium.setSize(100, 50);
			medium.setLocation(120, 10);
			medium.setToolTipText("Arena Size");
			this.add(medium);
			selected = medium;
			
			large = new JButton("Large");
			large.addActionListener(this);
			large.setVisible(true);
			large.setSize(100, 50);
			large.setLocation(120, 10);
			large.setToolTipText("Arena Size");
			
			small = new JButton("Small");
			small.addActionListener(this);
			small.setVisible(true);
			small.setSize(100, 50);
			small.setLocation(120, 10);
			small.setToolTipText("Arena Size");
			
			this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			this.setTitle("Space Server Options");
	    	this.setLayout(null);
	    	this.setSize(780, 100);
	        this.setLocationRelativeTo(null);
	        this.setResizable(false);
			this.setFocusable(true);
			this.addKeyListener(this);
			this.validate();
			this.repaint();
	        this.setVisible(true);
			this.requestFocus();
		}
		@Override
		public void keyPressed(KeyEvent e) {
		}
		@Override
		public void keyReleased(KeyEvent e) {
			if(e.getKeyCode()==KeyEvent.VK_BACK_SPACE) {
				System.exit(0);
			}
		}
		@Override
		public void keyTyped(KeyEvent e) {
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			if(e.getSource()==startserver) {
				String newTitle = "Space Console  (Server Created)";
		    try {
		      newTitle += "ip= " + InetAddress.getLocalHost().getHostAddress();
		    } catch (UnknownHostException e1) {
		      e1.printStackTrace();
		    }
		    console.setTitle(newTitle);
				if(selected == small) {
					setSizeOfWorld(1000, 800);
				}
				if(selected == medium) {
					setSizeOfWorld(1500, 1300);
				}
				if(selected == large) {
					setSizeOfWorld(2000, 1800);
				}
				this.setVisible(false);
//				mframe.setVisible(true);
				console.setVisible(true);
			}
			if(e.getSource()==small) {
				this.remove(small);
				this.add(medium);
				selected = medium;
			}
			if(e.getSource()==medium) {
				this.remove(medium);
				this.add(large);
				selected = large;
			}
			if(e.getSource()==large) {
				this.remove(large);
				this.add(small);
				selected = small;
			}
			this.repaint();
		}
	}
	public void buildWall(int x, int y, int w, int h) {
		if(!wallcollides(new Rectangle(x, y, w, h))) {
			Wall newwall = new Wall(x, y, w, h);
			walls.add(newwall);
		}
	}
	public Connection getConnection(Color c) {
		for(int a=0; a<connections.size(); a++) {
			if(connections.get(a).player.equals(c)) {
				return connections.get(a);
			}
		}
		return null;
	}
	public void baseAttacked(Base b) {
		Connection con = getConnection(b.player);
		if(con !=null && !con.dc){ 
			con.send(this.baseattacked);
		}
	}
}
