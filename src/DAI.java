import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

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
    static String dm_name = "Valve";
    
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
    static class Scale extends ODF {
        public Scale () {
            super("Scale");
        }
        public void pull(JSONArray data) {
            System.out.println("Size: "+ data.toString());
            /* parse data from packet, assign to every yi */
        	if(selected && !suspended) {
        		IDA.write("Scale", (float) data.getDouble(0));
        	}
        }
    }
    static class Angle extends ODF {
        public Angle () {
            super("Angle");
        }
        public void pull(JSONArray data) {
        	if(selected && !suspended) {
                IDA.write("Angle", (float) data.getDouble(0));
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
            new Scale(),
            new Angle()
        );
    }
    
    /* IDA Class */
    public static class IDA extends PApplet{
        final int WINDOW_SIZE = 800;
        final int EDGE_LENGTH = WINDOW_SIZE / 8;
        final int MAX_LAYERS = 3;
        final float EDGE_WEIGHT = 0.5f;
        final float delay = 10;
        
        float current_scale;
        float current_angle;
        
        static final String[] df_list = new String[]{"Scale", "Angle"};
        static final HashMap<String, Float> feature_map = new HashMap<String, Float>();
        
        HE_Mesh mesh;
        WB_Render render;
        WB_Frame wb_frame;
        
        
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


        public void setup() {
            frameRate(30);
            
            feature_map.put("Scale", 0f);
            feature_map.put("Angle", 0f);
            
            frame.setTitle(d_name);
            
            current_scale = 0;
            current_angle = 0;
        }


        public void draw() {
            current_scale = feature_map.get("Scale");
            
//            if (current_scale == 0) {
//                // no graph to draw
//                return;
//            }

            float target_angle = map(feature_map.get("Angle"), 0, 1, 0.1f, 1);
            current_angle += (target_angle - current_angle) / delay;
            
            // add node and lines into frame
            wb_frame = new WB_Frame();
            int current_layers = (int)(current_scale * MAX_LAYERS);
            int left_offset = (EDGE_LENGTH * current_layers);
            
            // put points
            for (int layer = 0; layer <= 2 * current_layers; layer++) {
                float pane_x = current_angle * (layer * EDGE_LENGTH - left_offset);
                for (float p = 0; p < 6; p++) {
                    double point_x = EDGE_LENGTH * cos(p * PI / 3);
                    double point_y = EDGE_LENGTH * sin(p * PI / 3);
                    if (layer % 2 == 1) {
                        point_x *= 2 - current_angle;
                        point_y *= 2 - current_angle;
                    }
                    wb_frame.addNode(pane_x, point_x, point_y, EDGE_WEIGHT);
                }
                wb_frame.addNode(pane_x, 0, 0, EDGE_WEIGHT);
            }
            wb_frame.addNode(-(current_angle * left_offset + EDGE_LENGTH), 0, 0, EDGE_WEIGHT);
            wb_frame.addNode( (current_angle * left_offset + EDGE_LENGTH), 0, 0, EDGE_WEIGHT);
            
            // put edges
            // pane internal edges
            for (int layer = 0; layer <= 2 * current_layers; layer++) {
                for (int p = 0; p < 6; p++) {
                    wb_frame.addStrut(layer * 7 + p, layer * 7 + (p + 1) % 6);
                    wb_frame.addStrut(layer * 7 + p, layer * 7 + 6);
                }
            }
            
            for (int layer = 0; layer < 2 * current_layers; layer++) {
                for (int p = 0; p < 6; p++) {
                    wb_frame.addStrut(layer * 7 + p, (layer + 1) * 7 + p);
                }
            }
            
            for (int p = 0; p < 6; p++) {
                wb_frame.addStrut(2 * current_layers * 7 + 7, p);
                wb_frame.addStrut(2 * current_layers * 7 + 8, 2 * current_layers * 7 + p);
            }
            wb_frame.addStrut(2 * current_layers * 7 + 7, 2 * current_layers * 7 + 8);

            HEC_FromFrame creator=new HEC_FromFrame();
            creator.setFrame(wb_frame);
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
            
            background(120);
            //lights();      //set the lighting ambience
            
            translate(400, 400, 0);
            rotateY(mouseX*1.0f/width*TWO_PI);    //rotate view angle by mouse
            rotateX(mouseY*1.0f/height*TWO_PI);
            noStroke();
            render.drawFaces(mesh);    //render the shape to screen
            stroke(0);
            render.drawEdges(mesh);
        }

        public void settings() {
            size(WINDOW_SIZE, WINDOW_SIZE, OPENGL);
            smooth(8);
        }
        
        public void iot_app() {
            PApplet.runSketch(new String[]{d_name}, this);
        };

        static private void logging (String message) {
            System.out.println("[Valve] " + message);
        }
    }
}
