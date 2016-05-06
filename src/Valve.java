import processing.core.PApplet;
import wblut.geom.WB_Frame;
import wblut.hemesh.HEC_FromFrame;
import wblut.hemesh.HET_Diagnosis;
import wblut.hemesh.HE_Mesh;
import wblut.processing.WB_Render;


@SuppressWarnings("serial")
public class Valve extends PApplet {
    HE_Mesh mesh;
    WB_Render render;
    WB_Frame frame;
    float[][]vertices;

    float fullxzLen;
    float fullyLen;

    float angle;    //angle for every point of hexagon
    float radius;  //distance from hexagon center to vertex
    float yShrink;

    //float ypos0=-200;
    //float ypos1=-100;
    //float ypos2=-50;
    //float ypos3=0;
    //float ypos4=100;
    //float ypos5=200;

    float ypos0=-450;    //y-coord for every layer
    float ypos1=-300;
    float ypos2=-200;
    float ypos3=-100;
    float ypos4=0;
    float ypos5=100;
    float ypos6=200;
    float ypos7=300;
    float ypos8=450;


    public void setup() {


        frameRate(30);
        // Creates a mesh from a frame. A WB_Frame is a collection of points and a list of 
        // indexed connections.

        //Array of all points
        vertices=new float[51][3];    //all the vertices

        setInitialPoints();    //set initial position of every vertex
        connection();    //connect struts between points


        HEC_FromFrame creator=new HEC_FromFrame();    //creat shape from frame
        creator.setFrame(frame);
        //alternatively you can specify a HE_Mesh instead of a WB_Frame.
        creator.setStrutRadius(6);// strut radius
        creator.setStrutFacets(6);// number of faces in the struts, min 3, max whatever blows up the CPU
        creator.setAngleOffset(0.25);// rotate the struts by a fraction of a facet. 0 is no rotation, 1 is a rotation over a full facet. More noticeable for low number of facets.
        creator.setMinimumBalljointAngle(TWO_PI/3.0);//Threshold angle to include sphere in joint.
        //  creator.setMaximumStrutLength(30);//divide strut into equal parts if larger than maximum length.
        creator.setCap(true); //cap open endpoints of struts?
        //  creator.setTaper(true);// allow struts to have different radii at each end?
        //  creator.setCreateIsolatedNodes(false);// create spheres for isolated points?
        //  creator.setUseNodeValues(true);// use the value of the WB_Node as scaling factor, only useful if the frame was created using addNode().
        mesh=new HE_Mesh(creator);
        HET_Diagnosis.validate(mesh);
        render=new WB_Render(this);
    }


    public void draw() {
        background(120);
        //lights();      //set the lighting ambience
        translate(400, 400, 0);
        rotateY(mouseX*1.0f/width*TWO_PI);    //rotate view angle by mouse
        rotateX(mouseY*1.0f/height*TWO_PI);
        noStroke();
        render.drawFaces(mesh);    //render the shape to screen
        stroke(0);
        render.drawEdges(mesh);

        setNewPointsPos(map((float) Math.sin(frameCount*0.06),-1f,1f,0.5f,1.5f));    //reset points position
        connection();    //connect points
        //  println(yShrink);
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
        mesh=new HE_Mesh(creator);
        //  HET_Diagnosis.validate(mesh);
        render=new WB_Render(this);

        //  saveFrame("film/#####.png");


    }


    public void connection() {
        frame=new WB_Frame();

        for (int i=0; i<vertices.length; i++) {
            frame.addNode(vertices[i][0], vertices[i][1], vertices[i][2], .5);
        }

        for (int i=2; i<=7; i++) {
            frame.addStrut(1, i);
            frame.addStrut(0, i);
            if (i<7) {
                frame.addStrut(i, i+1);
            }
        }
        frame.addStrut(2, 7);


        //soft connection 1
        for (int i=8; i<=13; i++) {
            frame.addStrut(i, i-6);
            frame.addStrut(i, i+7);
            if (i<13) {
                frame.addStrut(i, i-5);
                frame.addStrut(i, i+8);
            }
        }
        frame.addStrut(13, 2);
        frame.addStrut(13, 15);

        for (int i=2; i<=7; i++) {
            frame.addStrut(i, i+13);
        }

        //hard connection 1
        for (int i=15; i<=20; i++) {
            frame.addStrut(14, i);
            if (i<20) {
                frame.addStrut(i, i+1);
            }
        }
        frame.addStrut(15, 20);


        //soft connection 2
        for (int i=21; i<=26; i++) {
            frame.addStrut(i, i-6);
            frame.addStrut(i, i+7);
            if (i<26) {
                frame.addStrut(i, i-5);
                frame.addStrut(i, i+8);
            }
        }
        frame.addStrut(26, 15);
        frame.addStrut(26, 28);

        for (int i=15; i<=20; i++) {
            frame.addStrut(i, i+13);
        }

        //hard connection 2
        for (int i=28; i<=33; i++) {
            frame.addStrut(27, i);
            if (i<33) {
                frame.addStrut(i, i+1);
            }
        }
        frame.addStrut(28, 33);

        //soft connection 3
        for (int i=34; i<=39; i++) {
            frame.addStrut(i, i-6);
            frame.addStrut(i, i+7);
            if (i<39) {
                frame.addStrut(i, i-5);
                frame.addStrut(i, i+8);
            }
        }
        frame.addStrut(39, 28);
        frame.addStrut(39, 41);

        for (int i=28; i<=33; i++) {
            frame.addStrut(i, i+13);
        }


        //hard connection vertics
        for (int i=41; i<=46; i++) {
            frame.addStrut(i, i-13);
        }

        for (int i=41; i<46; i++) {
            frame.addStrut(i, i+1);
        }
        frame.addStrut(41, 46);

        for (int i=41; i<=46; i++) {
            frame.addStrut(40, i);
            frame.addStrut(47, i);
        }

        //add 48
        frame.addStrut(8, 48);
        frame.addStrut(9, 48);
        frame.addStrut(10, 48);
        frame.addStrut(11, 48);
        frame.addStrut(12, 48);
        frame.addStrut(13, 48);

        //add 49
        frame.addStrut(21, 49);
        frame.addStrut(22, 49);
        frame.addStrut(23, 49);
        frame.addStrut(24, 49);
        frame.addStrut(25, 49);
        frame.addStrut(26, 49);

        //add 50
        frame.addStrut(34, 50);
        frame.addStrut(35, 50);
        frame.addStrut(36, 50);
        frame.addStrut(37, 50);
        frame.addStrut(38, 50);
        frame.addStrut(39, 50);

    }

    public void setInitialPoints() {
        vertices[0][0]=0;
        vertices[0][1]=ypos0;
        vertices[0][2]=0;

        vertices[1][0]=0;
        vertices[1][1]=ypos1;
        vertices[1][2]=0;

        radius=160;
        angle=0;
        for (int i=2; i<=7; i++) {
            vertices[i][0]=cos(angle)*radius;
            vertices[i][1]=ypos1;
            vertices[i][2]=sin(angle)*radius;
            angle+=PI/3;
        }

        fullxzLen=dist(0, 0, vertices[2][0], vertices[2][2]);
        fullyLen=ypos2-ypos1;

        for (int i=8; i<13; i++) {
            vertices[i][0]=(vertices[i-6][0]+vertices[i-5][0])/2;
            vertices[i][1]=ypos2;
            vertices[i][2]=(vertices[i-6][2]+vertices[i-5][2])/2;
        }

        vertices[13][0]=(vertices[7][0]+vertices[2][0])/2;
        vertices[13][1]=ypos2;
        vertices[13][2]=(vertices[7][2]+vertices[2][2])/2;


        vertices[14][0]=0;
        vertices[14][1]=ypos3;
        vertices[14][2]=0;

        angle=0;
        for (int i=15; i<=20; i++) {
            vertices[i][0]=cos(angle)*radius;
            vertices[i][1]=ypos3;
            vertices[i][2]=sin(angle)*radius;
            angle+=PI/3;
        }


        for (int i=21; i<26; i++) {
            vertices[i][0]=(vertices[i-6][0]+vertices[i-5][0])/2;
            vertices[i][1]=ypos4;
            vertices[i][2]=(vertices[i-6][2]+vertices[i-5][2])/2;
        }

        vertices[26][0]=(vertices[7][0]+vertices[2][0])/2;
        vertices[26][1]=ypos4;
        vertices[26][2]=(vertices[7][2]+vertices[2][2])/2;

        //add layer 5
        vertices[27][0]=0;
        vertices[27][1]=ypos5;
        vertices[27][2]=0;

        angle=0;
        for (int i=28; i<=33; i++) {
            vertices[i][0]=cos(angle)*radius;
            vertices[i][1]=ypos5;
            vertices[i][2]=sin(angle)*radius;
            angle+=PI/3;
        }

        //add layer 6
        for (int i=34; i<39; i++) {
            vertices[i][0]=(vertices[i-6][0]+vertices[i-5][0])/2;
            vertices[i][1]=ypos6;
            vertices[i][2]=(vertices[i-6][2]+vertices[i-5][2])/2;
        }

        vertices[39][0]=(vertices[7][0]+vertices[2][0])/2;
        vertices[39][1]=ypos6;
        vertices[39][2]=(vertices[7][2]+vertices[2][2])/2;  

        //add layer 7
        vertices[40][0]=0;
        vertices[40][1]=ypos7;
        vertices[40][2]=0;

        angle=0;
        for (int i=41; i<=46; i++) {
            vertices[i][0]=cos(angle)*radius;
            vertices[i][1]=ypos7;
            vertices[i][2]=sin(angle)*radius;
            angle+=PI/3;
        }


        vertices[47][0]=0;
        vertices[47][1]=ypos8;
        vertices[47][2]=0;

        //add 48
        vertices[48][0]=0;
        vertices[48][1]=ypos2;
        vertices[48][2]=0;

        //add 49
        vertices[49][0]=0;
        vertices[49][1]=ypos4;
        vertices[49][2]=0;

        //add 50
        vertices[50][0]=0;
        vertices[50][1]=ypos6;
        vertices[50][2]=0;

        fullxzLen=dist(0, 0, vertices[2][0], vertices[2][2]);
    }

    public void setNewPointsPos(float factor) {

        yShrink=fullyLen-sqrt(sq(fullyLen)-sq(fullxzLen*(1-factor)));
        println(yShrink);

        vertices[0][0]=0;
        vertices[0][1]=ypos0+yShrink*2;
        vertices[0][2]=0;

        vertices[1][0]=0;
        vertices[1][1]=ypos1+yShrink*2;
        vertices[1][2]=0;

        radius=160;
        angle=0;
        for (int i=2; i<=7; i++) {
            vertices[i][0]=cos(angle)*radius;
            vertices[i][1]=ypos1+yShrink*2;
            vertices[i][2]=sin(angle)*radius;
            angle+=PI/3;
        }

        for (int i=8; i<13; i++) {
            vertices[i][0]=(vertices[i-6][0]+vertices[i-5][0])/2*factor;
            vertices[i][1]=ypos2+yShrink;
            vertices[i][2]=(vertices[i-6][2]+vertices[i-5][2])/2*factor;
        }

        vertices[13][0]=(vertices[7][0]+vertices[2][0])/2*factor;
        vertices[13][1]=ypos2+yShrink;
        vertices[13][2]=(vertices[7][2]+vertices[2][2])/2*factor;


        //vertices[14][0]=0;
        //vertices[14][1]=ypos3;
        //vertices[14][2]=0;

        //angle=0;
        //for (int i=15; i<=20; i++) {
        //  vertices[i][0]=cos(angle)*radius;
        //  vertices[i][1]=ypos3;
        //  vertices[i][2]=sin(angle)*radius;
        //  angle+=PI/3;
        //}

        vertices[14][0]=0;
        vertices[14][1]=ypos3+yShrink;
        vertices[14][2]=0;

        angle=0;
        for (int i=15; i<=20; i++) {
            vertices[i][0]=cos(angle)*radius;
            vertices[i][1]=ypos3+yShrink;
            vertices[i][2]=sin(angle)*radius;
            angle+=PI/3;
        }


        //for (int i=21; i<26; i++) {
        //  vertices[i][0]=(vertices[i-6][0]+vertices[i-5][0])/2*factor;
        //  vertices[i][1]=ypos4-yShrink*1;
        //  vertices[i][2]=(vertices[i-6][2]+vertices[i-5][2])/2*factor;
        //}

        //vertices[26][0]=(vertices[20][0]+vertices[15][0])/2*factor;
        //vertices[26][1]=ypos4-yShrink*1;
        //vertices[26][2]=(vertices[20][2]+vertices[15][2])/2*factor;


        for (int i=21; i<26; i++) {
            vertices[i][0]=(vertices[i-6][0]+vertices[i-5][0])/2*factor;
            vertices[i][1]=ypos4;
            vertices[i][2]=(vertices[i-6][2]+vertices[i-5][2])/2*factor;
        }

        vertices[26][0]=(vertices[20][0]+vertices[15][0])/2*factor;
        vertices[26][1]=ypos4;
        vertices[26][2]=(vertices[20][2]+vertices[15][2])/2*factor;

        vertices[27][0]=0;
        vertices[27][1]=ypos5-yShrink;
        vertices[27][2]=0;

        angle=0;
        for (int i=28; i<=33; i++) {
            vertices[i][0]=cos(angle)*radius;
            vertices[i][1]=ypos5-yShrink;
            vertices[i][2]=sin(angle)*radius;
            angle+=PI/3;
        }



        for (int i=34; i<39; i++) {
            vertices[i][0]=(vertices[i-6][0]+vertices[i-5][0])/2*factor;
            vertices[i][1]=ypos6+yShrink;
            vertices[i][2]=(vertices[i-6][2]+vertices[i-5][2])/2*factor;
        }

        vertices[39][0]=(vertices[7][0]+vertices[2][0])/2*factor;
        vertices[39][1]=ypos6+yShrink;
        vertices[39][2]=(vertices[7][2]+vertices[2][2])/2*factor;


        vertices[40][0]=0;
        vertices[40][1]=ypos7-yShrink*2;
        vertices[40][2]=0;

        angle=0;
        for (int i=41; i<=46; i++) {
            vertices[i][0]=cos(angle)*radius;
            vertices[i][1]=ypos7-yShrink*2;
            vertices[i][2]=sin(angle)*radius;
            angle+=PI/3;
        }

        vertices[47][0]=0;
        vertices[47][1]=ypos8-yShrink*2;
        vertices[47][2]=0;

        //add 48
        vertices[48][0]=0;
        vertices[48][1]=ypos2+yShrink;
        vertices[48][2]=0;

        //add 49
        vertices[49][0]=0;
        vertices[49][1]=ypos4;
        vertices[49][2]=0;

        //add 50
        vertices[50][0]=0;
        vertices[50][1]=ypos6+yShrink;
        vertices[50][2]=0;

    }

    public void settings() {
        size(800, 800, OPENGL);
        smooth(8);
    }

    public static void main(String[] args) {
        PApplet.main(new String[] { "Valve" });
    }

    static private void logging (String message) {
        System.out.println("[Valve] " + message);
    }
}
