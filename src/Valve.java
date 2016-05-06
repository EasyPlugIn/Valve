import java.util.HashMap;

import processing.core.PApplet;
import wblut.geom.WB_Frame;
import wblut.hemesh.HEC_FromFrame;
import wblut.hemesh.HE_Mesh;
import wblut.processing.WB_Render;


@SuppressWarnings("serial")
public class Valve extends PApplet {
    final int WINDOW_SIZE = 800;
    final int EDGE_LENGTH = WINDOW_SIZE / 8;
    final int MAX_LAYERS = 3;
    final float EDGE_WEIGHT = 0.5f;
    final float delay = 10;
    
    float current_scale;
    float current_angle;
    
    static final String dm_name = Valve.class.getSimpleName();
    static final String[] df_list = new String[]{"Scale", "Angle"};
    static final HashMap<String, Float> feature_map = new HashMap<String, Float>();
    
    HE_Mesh mesh;
    WB_Render render;
    WB_Frame frame;
    
    
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
        
        current_scale = 0;
        current_angle = 0;
    }


    public void draw() {
        current_scale = feature_map.get("Scale");
        
//        if (current_scale == 0) {
//            // no graph to draw
//            return;
//        }

        float target_angle = map(feature_map.get("Angle"), 0, 1, 0.1f, 1);
        current_angle += (target_angle - current_angle) / delay;
        
        // add node and lines into frame
        frame = new WB_Frame();
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
                frame.addNode(pane_x, point_x, point_y, EDGE_WEIGHT);
            }
            frame.addNode(pane_x, 0, 0, EDGE_WEIGHT);
        }
        frame.addNode(-(current_angle * left_offset + EDGE_LENGTH), 0, 0, EDGE_WEIGHT);
        frame.addNode( (current_angle * left_offset + EDGE_LENGTH), 0, 0, EDGE_WEIGHT);
        
        // put edges
        // pane internal edges
        for (int layer = 0; layer <= 2 * current_layers; layer++) {
            for (int p = 0; p < 6; p++) {
                frame.addStrut(layer * 7 + p, layer * 7 + (p + 1) % 6);
                frame.addStrut(layer * 7 + p, layer * 7 + 6);
            }
        }
        
        for (int layer = 0; layer < 2 * current_layers; layer++) {
            for (int p = 0; p < 6; p++) {
                frame.addStrut(layer * 7 + p, (layer + 1) * 7 + p);
            }
        }
        
        for (int p = 0; p < 6; p++) {
            frame.addStrut(2 * current_layers * 7 + 7, p);
            frame.addStrut(2 * current_layers * 7 + 8, 2 * current_layers * 7 + p);
        }
        frame.addStrut(2 * current_layers * 7 + 7, 2 * current_layers * 7 + 8);

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
        DAI.init(dm_name, df_list);
    }

    public static void main(String[] args) {
        PApplet.main(new String[] {dm_name});
    }

    static private void logging (String message) {
        System.out.println("[Valve] " + message);
    }
}
