import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.awt.*;
import org.json.JSONArray;
import org.json.JSONObject;

import processing.core.PApplet;
import wblut.geom.WB_Frame;
import wblut.hemesh.HEC_FromFrame;
import wblut.hemesh.HE_Mesh;
import wblut.processing.WB_Render;
@SuppressWarnings("serial")
public class DAI implements DAN.DAN2DAI {
	static DAI dai = new DAI();
	static IDA ida = new IDA();
    static DAN dan = new DAN();
    
	static abstract class DF {
        public DF (String name) {
            this.name = name;
        }
        public String name;
        public boolean selected;
    }
	
	static abstract class IDF extends DF {
        public IDF (String name) {
            super(name);
        }
    }
	
    static abstract class ODF extends DF {
        public ODF (String name) {
            super(name);
        }
        abstract public void pull(JSONArray data);
    }
    
    static abstract class Command {
        public Command(String name) {
            this.name = name;
        }
        public String name;
        abstract public void run(JSONArray dl_cmd_params, JSONArray ul_cmd_params);
    }
    
    static ArrayList<DF> df_list = new ArrayList<DF>();
    static ArrayList<Command> cmd_list = new ArrayList<Command>();
    static boolean suspended = true;
    static final String config_filename = "config.txt";
    static String d_name = "";
    static String info = "";
    
    
    static void add_df (DF... dfs) {
        for (DF df: dfs) {
            df_list.add(df);
        }
    }
    
    static void add_command (Command... cmds) {
        for (Command cmd: cmds) {
            cmd_list.add(cmd);
        }
    }
    
    private static boolean is_selected(String df_name) {
        for (DF df: df_list) {
            if (df_name.equals(df.name)) {
                return df.selected;
            }
        }
        System.out.println("Device feature" + df_name + "is not found");
        return false;
    }
 
    private static Command get_cmd(String cmd_name) {
        for(Command cmd: cmd_list) {
            if(cmd_name.equals(cmd.name) || cmd_name.equals(cmd.name + "_RSP")) {
                return cmd;
            }
        }
        System.out.println("Command" + cmd_name + "is not found");
        return null;
    }
    
    private static DF get_df(String df_name) {
        for(DF df: df_list) {
            if(df_name.equals(df.name)) {
                return df;
            }
        }
        System.out.println("Device feature" + df_name + "is not found");
        return null;
    }
    
    
    /* Default command-1: SET_DF_STATUS */
    static class SET_DF_STATUS extends Command {
        public SET_DF_STATUS() {
            super("SET_DF_STATUS");
        }
        public void run(final JSONArray df_status_list,
                         final JSONArray updated_df_status_list) {
            if(df_status_list != null && updated_df_status_list == null) {
            	final String flags = df_status_list.getString(0);
                for(int i = 0; i < flags.length(); i++) {
                    if(flags.charAt(i) == '0') {
                        df_list.get(i).selected = false;
                    } else {
                        df_list.get(i).selected = true;
                    }
                }
	            get_cmd("SET_DF_STATUS_RSP").run(
            		null,
            		new JSONArray(){{
		            	put(flags);
		            }}
        		);
            }
            else if(df_status_list == null && updated_df_status_list != null) {
            	dan.push(
                		"Control",
                		new JSONArray(){{
    	                	put("SET_DF_STATUS_RSP");
    	                	put(new JSONObject(){{
    	                		put("cmd_params", updated_df_status_list);
    	                	}});
                		}}
            		);
            } else {
                System.out.println("Both the df_status_list and the updated_df_status_list are null");
            }
        }
    }
    /* Default command-2: RESUME */
    static class RESUME extends Command {
        public RESUME() {
            super("RESUME");
        }
        public void run(final JSONArray dl_cmd_params,
                         final JSONArray exec_result) {
            if(dl_cmd_params != null && exec_result == null) {
            	suspended = false;
                get_cmd("RESUME_RSP").run(
                	null, 
                	new JSONArray(){{
                	    put("OK");
	            }});
            }
            else if(dl_cmd_params == null && exec_result != null) {
            	dan.push(
                		"Control",
                		new JSONArray(){{
    	                	put("RESUME_RSP");
    	                	put(new JSONObject(){{
    	                		put("cmd_params", exec_result);
    	                	}});
                		}}
            		);
            } else {
            	System.out.println("Both the dl_cmd_params and the exec_result are null!");
            }
        }
    }
    /* Default command-3: SUSPEND */
    static class SUSPEND extends Command {
        public SUSPEND() {
            super("SUSPEND");
        }
        public void run(JSONArray dl_cmd_params,
                         final JSONArray exec_result) {
            if(dl_cmd_params != null && exec_result == null) {
            	suspended = true;
                get_cmd("SUSPEND_RSP").run(
                	null, 
                	new JSONArray(){{
                		put("OK");
    	        }});
            }
            else if(dl_cmd_params == null && exec_result != null) {
            	dan.push(
                		"Control",
                		new JSONArray(){{
    	                	put("SUSPEND_RSP");
    	                	put(new JSONObject(){{
    	                		put("cmd_params", exec_result);
    	                	}});
                		}}
            		);
            } else {
            	System.out.println("Both the dl_cmd_params and the exec_result are null!");
            }
        }
    }
    
	public void add_shutdownhook() {
		Runtime.getRuntime().addShutdownHook(new Thread () {
            @Override
            public void run () {
            	deregister();
            }
        });
	}
	
	/* deregister() */
	public void deregister() {
		dan.deregister();
	}
	
	@Override
	public void pull(final String odf_name, final JSONArray data) {
		if (odf_name.equals("Control")) {
            final String cmd_name = data.getString(0);
            JSONArray dl_cmd_params = data.getJSONObject(1).getJSONArray("cmd_params");
            Command cmd = get_cmd(cmd_name);
            if (cmd != null) {
                cmd.run(dl_cmd_params, null);
                return;
            }
            
            /* Reports the exception to IoTtalk*/
            dan.push("Control", new JSONArray(){{
            	put("UNKNOWN_COMMAND");
            	put(new JSONObject(){{
            		put("cmd_params", new JSONArray(){{
            			put(cmd_name);
            		}});
            	}});
            }});
        } else {
        	ODF odf = ((ODF)get_df(odf_name));
        	if (odf != null) {
        		odf.pull(data);
                return;
            }
            
            /* Reports the exception to IoTtalk*/
            dan.push("Control", new JSONArray(){{
            	put("UNKNOWN_ODF");
            	put(new JSONObject(){{
            		put("cmd_params", new JSONArray(){{
            			put(odf_name);
            		}});
            	}});
            }});
        }
	}
    
    
   static private String get_config_ec () {
        try {
            /* assume that the config file has only one line,
             *  which is the IP address of the EC (without port number)*/
            BufferedReader br = new BufferedReader(new FileReader(config_filename));
            try {
                String line = br.readLine();
                if (line != null) {
                    return line;
                }
                return "localhost";
            } finally {
                br.close();
            }
        } catch (IOException e) {
            return "localhost";
        }
    }
	

    /* The main() function */
    public static void main(String[] args) {
        add_command(
            new SET_DF_STATUS(),
            new RESUME(),
            new SUSPEND()
        );
        init_cmds();
        init_dfs();
        final JSONArray df_name_list = new JSONArray();
        for(int i = 0; i < df_list.size(); i++) {
            df_name_list.put(df_list.get(i).name);
        }
        
        String endpoint = get_config_ec();
        if (!endpoint.startsWith("http://")) {
            endpoint = "http://" + endpoint;
        }
        if (endpoint.length() - endpoint.replace(":", "").length() == 1) {
            endpoint += ":9999";
        }
        
        JSONObject profile = new JSONObject() {{
            put("dm_name", dm_name); //deleted
            put("u_name", "yb");
            put("df_list", df_name_list);
            put("is_sim", false);
        }};
        
        String d_id = "";
        Random rn = new Random();
        for (int i = 0; i < 12; i++) {
            int a = rn.nextInt(16);
            d_id += "0123456789ABCDEF".charAt(a);
        }

        dan.init(dai, endpoint, d_id, profile);
        d_name = profile.getString("d_name");
        dai.add_shutdownhook();

        /* Performs the functionality of the IDA */
        ida.iot_app();             
    }
    
    /*--------------------------------------------------*/
    /* Customizable part */
    static String dm_name = "Skeleton";
    
    static class Mouse extends IDF {
        public Mouse () {
            super("Mouse");
        }
        public void push(double x, double y) {
        	if(selected && !suspended) {
	        	JSONArray data = new JSONArray();
	            data.put(x);
	            data.put(y);
	            dan.push(name, data);
        	}
        }
    }
    
    /* Declaration of ODF classes, generated by the DAC */
    static class Size extends ODF {
        public Size () {
            super("Size");
        }
        public void pull(JSONArray data) {
            System.out.println("Size: "+ data.toString());
            /* parse data from packet, assign to every yi */
        	if(selected && !suspended) {
            	if (IDA.size > 10) {
            		IDA.size = 10;
                }else
        		    IDA.size = (float) data.getDouble(0);
        	}
        	else {
                IDA.size = 0; // default value
        	}
        }
    }
    static class Angle extends ODF {
        public Angle () {
            super("Angle");
        }
        public void pull(JSONArray data) {
//        	System.out.println("Angle: "+ data.toString());
        	if(selected && !suspended) {
        	    //if(IDA.angle > 0.97f) IDA.angle = 0.97f;
        		logging("Init_angle: %f", IDA.angle);
        	    if (IDA.angle > 90) {
                    IDA.angle = IDA.angle%90;
                }
        	    IDA.angle = (float) data.getDouble(0);
        	    if((IDA.angle > 88.5f)&&(IDA.angle <= 90f)) IDA.angle = 88.5f;
        	    
        	}
        	else {
                IDA.angle = 0f;
        	}
        }
    }
    
    static class Color extends ODF {
        public Color () {
            super("Color-O");
        }
        public void pull(JSONArray data) {
        	if(selected && !suspended) {
        	    ida.color_r = (float) data.getDouble(0);
        	    ida.color_g = (float) data.getDouble(1);
        	    ida.color_b = (float) data.getDouble(2);
        	}
        	else {
        		ida.color_r = 0f;
        	    ida.color_g = 0f;
        	    ida.color_b = 0f;
        	}
        }
    }
    /* Initialization of command list and DF list, generated by the DAC */
    static void init_cmds () {
        add_command(
        //    new SAMPLE_COMMAND ()
        );
    }
    static void init_dfs () {
        add_df(
            new Size(),
            new Angle(),
            new Color()
        );
    }
    
    /* IDA Class */
    public static class IDA extends PApplet{
    	public static float angle;
		public static float size;
    	public float color_b;
		public float color_g;
		public float color_r;
		public float current_color_r;
    	public float current_color_g;
    	public float current_color_b;
    	float current_angle;
        float flexible_offset;
        float pane_x;
        int scale_size;
        final int width = 1920;
        final int height = 800;
        HE_Mesh mesh;
        WB_Render render;
        WB_Frame frame;
        static final String[] df_list = new String[]{"Size", "Angle", "Color-O"};
        static final HashMap<String, Float> feature_map = new HashMap<String, Float>();

        double point_size = 0.5;
        int basis_length = 100;
              
        static public void write (String feature, float para_data) {
            if (!feature_map.containsKey(feature)) {
                // error
                return;
            }
            
            if (para_data < 0 || para_data > 1) {
                // error
                return;
            }
            feature_map.put(feature, para_data);
        }

        int TEXT_SIZE = 15;
    	int text_lines;
    	public void stack_text (String format, Object... args) {
    	    if (format.equals("")) {
    	        text_lines = 0;
    	    }
    	    text(String.format(format, args), 0, height - text_lines * TEXT_SIZE);
    	    text_lines += 1;
    	    //fill(0);
    	}
        public void settings() {
        	size(width, height, P3D);
        	smooth(8);
        }
        
        public void setup() {
            frameRate(30);
            
            current_angle = 0;
            surface.setTitle(d_name);
        }


        public void draw() {
        	strokeWeight(3f);
//        	float target_angle = map(angle, 0, 90, 1, 0f);
    	    if (angle > 90) {
                angle = angle%90;
            }
    	    
    	    if (size > 10) {
                size = 10;
            }
        	current_angle = approximate(current_angle, angle);
        	logging("%f", current_angle);
            
        	current_color_r = approximate(current_color_r, color_r);
        	current_color_g = approximate(current_color_g, color_g);
        	current_color_b = approximate(current_color_b, color_b);
        	 // add node and lines into frame
            frame = new WB_Frame();
          
            //int current_layers = (int) (size * 3);
            //scale_size = (int) (size * 10);
            scale_size = (int)size;//sherry0818
            int base_offset = basis_length * scale_size;
            //flexible_offset = 1 + (current_angle * 90 - 0) * (-1f/90f);
            flexible_offset = 1 + (current_angle - 0) * (-1f/90f); //sherry 0818
            

            
            // put points
            for (int layer = 0; layer <= 2 * scale_size; layer++) {
                pane_x = flexible_offset * (layer * basis_length - base_offset);
                for (float p = 0; p < 6; p++) {
                    double point_x = basis_length * cos(p * PI / 3);
                    double point_y = basis_length * sin(p * PI / 3);
                    if (layer % 2 == 1) {
                        point_x = basis_length * cos(p * PI / 3) * (2 - flexible_offset);
                        point_y = basis_length * sin(p * PI / 3) * (2 - flexible_offset);
                    }
                    //layers external node
                    frame.addNode(pane_x, point_x, point_y, point_size);
                }
                //center node
                frame.addNode(pane_x, 0, 0, point_size);
            }
            //left endpoint node
            frame.addNode(-(flexible_offset * base_offset + basis_length), 0, 0, point_size);
            //right endpoint node
            frame.addNode( (flexible_offset * base_offset + basis_length), 0, 0, point_size);
            
            // put edges
            // pane internal edges
            for (int layer = 0; layer <= 2 * scale_size; layer++) {
                for (int p = 0; p < 6; p++) {
                    frame.addStrut(layer * 7 + p, layer * 7 + (p + 1) % 6);
                    frame.addStrut(layer * 7 + p, layer * 7 + 6);
                }
            }
            
            for (int layer = 0; layer < 2 * scale_size; layer++) {
                for (int p = 0; p < 6; p++) {
                    frame.addStrut(layer * 7 + p, (layer + 1) * 7 + p);
                }
            }
            
            for (int p = 0; p < 6; p++) {
                frame.addStrut(2 * scale_size * 7 + 7, p);
                frame.addStrut(2 * scale_size * 7 + 8, 2 * scale_size * 7 + p);
            }
            frame.addStrut(2 * scale_size * 7 + 7, 2 * scale_size * 7 + 8);
            

            HEC_FromFrame creator=new HEC_FromFrame();
            creator.setFrame(frame);
            //alternatively you can specify a HE_Mesh instead of a WB_Frame.
            creator.setStrutRadius(6);// strut radius
            creator.setStrutFacets(6);// number of faces in the struts, min 3, max whatever blows up the CPU
            //creator.setAngleOffset(0.25);// rotate the struts by a fraction of a facet. 0 is no rotation, 1 is a rotation over a full facet. More noticeable for low number of facets.
            creator.setMinimumBalljointAngle(TWO_PI/3.0);//Threshold angle to include sphere in joint.
            creator.setMaximumStrutLength(100);//divide strut into equal parts if larger than maximum length.
            creator.setCap(true); //cap open endpoints of struts?
            //  creator.setTaper(true);// allow struts to have different radii at each end?
            //  creator.setCreateIsolatedNodes(false);// create spheres for isolated points?
            //  creator.setUseNodeValues(true);// use the value of the WB_Node as scaling factor, only useful if the frame was created using addNode().
            

            mesh = new HE_Mesh(creator);
            //  HET_Diagnosis.validate(mesh);
            render = new WB_Render(this);
            
            background(255);
            //lights();      //set the lighting ambience

            //fill(0);
            //show info
//            textSize(8);
//            text(info, 0, height+8);
//            textSize(TEXT_SIZE);          
//            stack_text("");
//            stack_text("Color: (%.2f, %.2f, %.2f) (%.2f, %.2f, %.2f)", color_r, color_g, color_b, current_color_r, current_color_g, current_color_b);
//            stack_text("ODF Angle: %f (%f)", angle, current_angle);
//            stack_text("ODF Size: %f (%f)", size, size);
//            stack_text("Device name: %s", d_name);
//            fill(0);
            
            translate(width/2, height/2, 0);
            rotateY(mouseX*1.0f/width*TWO_PI);    //rotate view angle by mouse
            rotateX(mouseY*1.0f/height*TWO_PI);
            noStroke();
            render.drawFaces(mesh);    //render the shape to screen        
            stroke(current_color_r, current_color_g, current_color_b);
            render.drawEdges(mesh);

            
        }

        public void iot_app() {
            PApplet.runSketch(new String[]{d_name}, this);
        };
        
        

        public float approximate (float source, float target) {
    	    return source + (target - source) / 100;
    	}
    }

	static void logging (String format, Object... args) {
		logging(String.format(format, args));
	}
    
	static void logging (String message) {
		System.out.printf("[%s][CSMapi] %s%n", "DAI", message);
	}
}