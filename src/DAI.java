import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import DAN.DAN;

public class DAI {
	
	public static abstract class IDA_Manager {
	
		public abstract void search();
		public abstract void init();
		public abstract void connect();
		public abstract void read();
		public abstract void write(String frature, double data);
		public abstract void disconnect();
	
	}
	
	
	
	
	public static void init(final HashMap<String, Double> feature_map) {
		
		 final IDA_Manager valve_ida_manager =  new IDA_Manager() {

				@Override
				public void search() {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void init() {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void connect() {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void read() {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void write(String feature, double data) {
//				    if(feature_map.containsKey(feature))
//					    feature_map.put(feature, data);
//				    else
//				    	System.out.println("Feature '" + feature + "' not found!");
				}

				@Override
				public void disconnect() {
					// TODO Auto-generated method stub
					
				}
				
			};
		
			//DAI initialization
			
			final DAN.Subscriber odf_subscriber = new DAN.Subscriber () {
	        	public void odf_handler (String feature, DAN.ODFObject odf_object) {
	        		double newest = pull(odf_object);
	        		valve_ida_manager.write(feature, newest);
	            	//logging("new data: "+ odf_object.feature +", "+ newest.data);
	        	}
	        };
	        
	        DAN.Subscriber event_subscriber = new DAN.Subscriber() {
	        	public void odf_handler (String feature, DAN.ODFObject odf_object) {
	        		switch (odf_object.event) {
	        		case REGISTER_FAILED:
	        			//logging("Register failed: "+ odf_object.message);
	        			break;
	        		case REGISTER_SUCCEED:
	        			//logging("Register successed: "+ odf_object.message);
	        			Iterator it = feature_map.keySet().iterator();  
	        	        while(it.hasNext()) {  
	        	            String key = (String)it.next();
	        	            DAN.subscribe(key, odf_subscriber);
	        	        }  
	        			break;
	        		default:
	        			break;
	        		}
	        	}
	        };
		    

	        DAN.init("Valve", event_subscriber);
	        JSONObject profile = new JSONObject();
	        try {
		        profile.put("d_name", "Lbs001");
		        profile.put("dm_name", "Valve");
		        JSONArray feature_list = new JSONArray();
		        Iterator it = feature_map.keySet().iterator();  
    	        while(it.hasNext()) {  
    	            String key = (String)it.next();
    	            feature_list.put(key);
    	        }  
		        profile.put("df_list", feature_list);
		        profile.put("u_name", "yb");
		        profile.put("is_sim", false);
		        DAN.register("http://localhost:9999", "Lbs001", profile);
			} catch (JSONException e) {
				e.printStackTrace();
			}
	        
	        //logging("[Valve] EasyConnect Host: " + csmapi.ENDPOINT);
	        
	    	Runtime.getRuntime().addShutdownHook(new Thread () {
	        	@Override
	        	public void run () {
	               //logging("shutdown hook");
	                deregister();
	        	}
	        });
	}
	
	private static void deregister() {
		DAN.deregister();
	}
	
	private static double pull(DAN.ODFObject odf_object) {
		return odf_object.data.getDouble(0);
	}

	static String mac_addr_cache = "";
    static public String get_mac_addr () {
    	if (!mac_addr_cache.equals("")) {
    		logging("Mac address cache: "+ mac_addr_cache);
    		return mac_addr_cache;
    	}
    	
    	InetAddress ip;
    	try {
    		ip = InetAddress.getLocalHost();
    		System.out.println("Current IP address : " + ip.getHostAddress());
    		NetworkInterface network = NetworkInterface.getByInetAddress(ip);
    		byte[] mac = network.getHardwareAddress();
    		mac_addr_cache += String.format("%02X", mac[0]);
    		for (int i = 1; i < mac.length; i++) {
    			mac_addr_cache += String.format(":%02X", mac[i]);
    		}
    		logging(mac_addr_cache);
    		return mac_addr_cache;
    	} catch (UnknownHostException e) {
    		e.printStackTrace();
    	} catch (SocketException e){
    		e.printStackTrace();
    	}

		logging("Mac address cache retriving failed, use random string");
        Random rn = new Random();
        for (int i = 0; i < 12; i++) {
            int a = rn.nextInt(16);
            mac_addr_cache += "0123456789abcdef".charAt(a);
        }
        return mac_addr_cache;
    }

    static private String log_tag = "Valve";
    static private final String local_log_tag = "DAI";
    static private void logging (String message) {
		String padding = message.startsWith(" ") || message.startsWith("[") ? "" : " ";
        System.out.printf("[%s][%s]%s%s%n", log_tag, local_log_tag, padding, message);
    }
	
}
