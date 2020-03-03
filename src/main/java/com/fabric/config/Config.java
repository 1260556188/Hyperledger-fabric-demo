package com.fabric.config;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.helper.Utils;

import com.fabric.model.Org;
import com.fabric.model.UserContext;
import com.fabric.utils.Util;
/**
 * Config allows for a global config of the toolkit. Central location for all
 * toolkit configuration defaults.
 */

public class Config {

	public static Map<String, Channel> channelMap = new ConcurrentHashMap<>(100);

	public static String Path = "/home/ycy/workspace/fabric-samples/first-network/";
	
	private static final Log logger = LogFactory.getLog(Config.class);

	private static final String PROPBASE = "config.";
	private static String PATH = System.getProperty("user.dir");
	

	private static final String GOSSIPWAITTIME = PROPBASE + "GossipWaitTime";
	private static final String INVOKEWAITTIME = PROPBASE + "InvokeWaitTime";
	private static final String DEPLOYWAITTIME = PROPBASE + "DeployWaitTime";
	private static final String PROPOSALWAITTIME = PROPBASE + "ProposalWaitTime";

	private static final String ORGS = PROPBASE + "property.";
	private static final Pattern orgPat = Pattern.compile("^" + Pattern.quote(ORGS) + "([^\\.]+)\\.mspid$");

	private static final String BLOCKCHAINTLS = PROPBASE + "blockchain.tls";

	private static Config config;
	public static final Properties sdkProperties = new Properties();
	private final boolean runningTLS;
	private final boolean runningFabricCATLS;
	private final boolean runningFabricTLS;
	private static final HashMap<String, Org> sampleOrgs = new HashMap<>();
	//public  HashMap<String, Org> sampleOrgs = new HashMap<>();
	public boolean isRunningFabricTLS() {
		return runningFabricTLS;
	}

	private Config() {

		try {

			/**
			 * All the properties will be obtained from config.properties file
			 */

			sdkProperties.load(new FileInputStream(PATH + "/config2.properties"));

			

		} catch (IOException e) {
			// if not there no worries just use defaults
			logger.warn("Failed to load any configuration");
		} finally {			
			
			runningTLS = Boolean.parseBoolean(sdkProperties.getProperty(BLOCKCHAINTLS, "false"));
			runningFabricCATLS = runningTLS;
			runningFabricTLS = runningTLS;

			String property = sdkProperties.getProperty("config.Orgs");
			String[] split = property.split(",");
			//组织
			for (int i = 0; i < split.length; i++) {
				String orgtag = split[i];
				
				String orgname = sdkProperties.getProperty("config."+orgtag+".name");
				String orgmsp = sdkProperties.getProperty("config."+orgtag+".mspid");
				
				Org org = new Org(orgname,orgmsp);
				
				//peerAdmin
				String admin_pk = sdkProperties.getProperty("config."+orgtag+".admin_pk");
				String admin_cert = sdkProperties.getProperty("config."+orgtag+".admin_cert");;
				UserContext peerAdmin = new UserContext();
				File pkFolder = new File(Path + admin_pk);
				File[] pkFiles = pkFolder.listFiles();
				File certFolder = new File(Path + admin_cert);
				File[] certFiles = certFolder.listFiles();
				Enrollment enrollOrg = null;
				try {
					enrollOrg = Util.getEnrollment(Path + admin_pk, pkFiles[0].getName(),
							Path + admin_cert, certFiles[0].getName());
				} catch (NoSuchAlgorithmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvalidKeySpecException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (CryptoException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				peerAdmin.setEnrollment(enrollOrg);
				peerAdmin.setMspId(orgmsp);
				peerAdmin.setName("admin");
				org.setPeerAdmin(peerAdmin);
				//tls_ca
				String ca_tls_cert = sdkProperties.getProperty("config."+orgtag+".ca_tls_cert");
				org.setTls_ca_cert(Path+ca_tls_cert);
				//peer
				String peersPro = sdkProperties.getProperty("config."+orgtag+".Peers");
				String[] peers = peersPro.split(",");
				for (int j = 0; j < peers.length; j++) {
					String peertag = peers[j];
					String peername = sdkProperties.getProperty("config."+orgtag+"."+peertag+".name");
					String peerlocation = sdkProperties.getProperty("config."+orgtag+"."+peertag+".location");
					
					org.addPeerLocation(peername, peerlocation);
				}
				
				
				//order
				String ordersPro = sdkProperties.getProperty("config."+orgtag+".Orders");
				String[] orders = ordersPro.split(",");
				for (int j = 0; j < orders.length; j++) {
					String ordertag = orders[j];
					String ordername = sdkProperties.getProperty("config."+orgtag+"."+ordertag+".name");
					String orderloaction = sdkProperties.getProperty("config."+orgtag+"."+ordertag+".location");
					org.addOrdererLocation(ordername, orderloaction);
				}
				
				//ca
				String ca_location = sdkProperties.getProperty("config."+orgtag+".ca_location");
				String ca_cert = sdkProperties.getProperty("config."+orgtag+".ca_cert");
				Properties properties = new Properties();
				properties.setProperty("pemFile", Path+ca_cert);
				org.setCALocation(ca_location);
				org.setCAProperties(properties);
				
				sampleOrgs.put(orgname, org);
				System.out.println(split[i]);
			}
			
		}

	}

	private String grpcTLSify(String location) {
		location = location.trim();
		Exception e = Utils.checkGrpcUrl(location);
		if (e != null) {
			throw new RuntimeException(String.format("Bad  parameters for grpc url %s", location), e);
		}
		return runningFabricTLS ? location.replaceFirst("^grpc://", "grpcs://") : location;

	}

	private String httpTLSify(String location) {
		location = location.trim();

		return runningFabricCATLS ? location.replaceFirst("^http://", "https://") : location;
	}

	/**
	 * getConfig return back singleton for SDK configuration.
	 *
	 * @return Global configuration
	 */
	public static Config getConfig() {
		if (null == config) {
			config = new Config();
		}
		return config;

	}

	/**
	 * getProperty return back property for the given value.
	 *
	 * @param property
	 * @return String value for the property
	 */
	private String getProperty(String property) {

		String ret = sdkProperties.getProperty(property);

		if (null == ret) {
			logger.warn(String.format("No configuration value found for '%s'", property));
		}
		return ret;
	}



	public int getTransactionWaitTime() {
		return Integer.parseInt(getProperty(INVOKEWAITTIME));
	}

	public int getDeployWaitTime() {
		return Integer.parseInt(getProperty(DEPLOYWAITTIME));
	}

	public int getGossipWaitTime() {
		return Integer.parseInt(getProperty(GOSSIPWAITTIME));
	}

	public long getProposalWaitTime() {
		return Integer.parseInt(getProperty(PROPOSALWAITTIME));
	}

	public Collection<Org> getSampleOrgs() {
		return Collections.unmodifiableCollection(sampleOrgs.values());
	}

	public Org getSampleOrg(String name) {
		return sampleOrgs.get(name);

	}
	public Map getSampleOrgs1() {
		return sampleOrgs;

	}
	public Properties getPeerProperties(String name) {

		return getEndPointProperties("peer", name);

	}

	public Properties getOrdererProperties(String name) {

		return getEndPointProperties("orderer", name);

	}

	private Properties getEndPointProperties(final String type, final String name) {

		final String domainName = getDomainName(name);

		File cert = Paths.get(getChannelPath(), "crypto-config/ordererOrganizations".replace("orderer", type),
				domainName, type + "s", name, "tls/server.crt").toFile();
		if (!cert.exists()) {
			throw new RuntimeException(String.format("Missing cert file for: %s. Could not find at location: %s", name,
					cert.getAbsolutePath()));
		}

		Properties ret = new Properties();
		ret.setProperty("pemFile", cert.getAbsolutePath());

		ret.setProperty("hostnameOverride", name);
		ret.setProperty("sslProvider", "openSSL");
		ret.setProperty("negotiationType", "TLS");

		return ret;
	}

	public Properties getEventHubProperties(String name) {

		return getEndPointProperties("peer", name); // uses same as named peer

	}

	public String getChannelPath() {

//		/**
//		 * for loading properties from hyperledger.properties file
//		 */
//		Properties hyperproperties = new Properties();
//		try {
//			hyperproperties.load(new FileInputStream("src/main/resources/hyperledger.properties"));
//		} catch (IOException e1) {
//			e1.printStackTrace();
//		}
		return PATH+"/artifacts/channel";

	}

	private String getDomainName(final String name) {
		int dot = name.indexOf(".");
		if (-1 == dot) {
			return null;
		} else {
			return name.substring(dot + 1);
		}

	}

	public Properties getOrdererProperties(String peerWithOrg, String orderName) {
		//System.out.println(orderName);
		String[] ordertag = orderName.split("\\.");
		//System.out.println(ordertag.length);
		String permfile = getProperty("config."+peerWithOrg+"."+ordertag[0]+".tls_permfile");
		File file = new File(Path+permfile);
		if(!file.exists()) {
			throw new RuntimeException("could not find the ca tls certificate");
		}
		Properties ret = new Properties();
		ret.setProperty("pemFile", Path+permfile);
		ret.setProperty("sslProvider", "openSSL");
		ret.setProperty("negotiationType", "TLS");
		
		return ret;
	}

}

