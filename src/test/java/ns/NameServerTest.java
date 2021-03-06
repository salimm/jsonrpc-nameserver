package test.java.ns;

import java.io.FileNotFoundException;
import java.net.Inet4Address;
import java.sql.SQLException;
import java.util.ArrayList;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.lessrpc.common.errors.ApplicationSpecificErrorException;
import org.lessrpc.common.errors.DatabaseNotSupported;
import org.lessrpc.common.info.EnvironmentInfo;
import org.lessrpc.common.info.SerializationFormat;
import org.lessrpc.common.info.ServiceInfo;
import org.lessrpc.common.info.ServiceProviderInfo;
import org.lessrpc.common.info.ServiceSupportInfo;
import org.lessrpc.common.services.NameServerFunctions;
import org.lessrpc.common.services.NameServerServices;
import org.lessrpc.ns.core.db.DBFactory;
import org.lessrpc.ns.core.server.NameServer;
import org.lessrpc.stub.java.stubs.ClientStub;
import org.lessrpc.stub.java.stubs.NSClient;

import me.salimm.allconfig.core.Config;
import me.salimm.allconfig.core.errors.PrefixNotANestedConfigException;
import me.salimm.allconfig.core.types.XMLConfig;

public class NameServerTest implements NameServerServices {

	private NameServer ns;

	private ServiceProviderInfo nsSPInfo;

	private final ServiceProviderInfo sampleProvider = new ServiceProviderInfo("test", 5,
			EnvironmentInfo.currentEnvInfo());

	@Before
	public void runNS() throws ClassNotFoundException, FileNotFoundException, SQLException, DatabaseNotSupported,
			PrefixNotANestedConfigException, Exception {
		Config conf = new XMLConfig("conf.xml");
		ns = new NameServer(7364, DBFactory.getDBInfo(conf), DBFactory.getDBUtils(conf));
		ns.start();
		ns.reset();
		nsSPInfo = new ServiceProviderInfo(Inet4Address.getLocalHost().getHostAddress(), 7364,
				EnvironmentInfo.currentEnvInfo());

	}

	@Test
	public void testPing() throws Exception {
		ClientStub client = new ClientStub(new ArrayList<>());
		boolean flag = client.ping(nsSPInfo);
		Assert.assertTrue(flag);
	}

	@Test
	public void testInfo() throws Exception {
		ClientStub client = new ClientStub(new ArrayList<>());
		ServiceProviderInfo info = client.getInfo("localhost", 7364);
		Assert.assertEquals(nsSPInfo, info);
	}

	@Test
	public void testSupportedService() throws Exception {
		ClientStub client = new ClientStub(new ArrayList<>());
		ServiceSupportInfo info = client.getServiceSupport(nsSPInfo, NameServerServices.REGISTER.getInfo());
		Assert.assertNotNull(info);

		Assert.assertEquals(NameServerServices.REGISTER.getInfo(), info.getService());
		Assert.assertEquals(nsSPInfo, info.getProvider());
		Assert.assertEquals(1, info.getSerializers().length);
	}

	@Test
	public void testRegister() throws Exception {
		ServiceInfo<Integer> service = new ServiceInfo<Integer>("test", 1);
		NSClient client = new NSClient(nsSPInfo, new ArrayList<>());
		boolean flag = client.register(new ServiceSupportInfo(service, sampleProvider,
				new SerializationFormat[] { SerializationFormat.defaultFotmat() }));

		Assert.assertEquals(true, flag);
	}

	@Test
	public void testRegisterDuplicate() throws Exception {
		ServiceInfo<Integer> service = new ServiceInfo<Integer>("test", 1);
		NSClient client = new NSClient(nsSPInfo, new ArrayList<>());
		boolean flag = client.register(new ServiceSupportInfo(service, sampleProvider,
				new SerializationFormat[] { SerializationFormat.defaultFotmat() }));
		Assert.assertEquals(true, flag);

		try {
			flag = client.register(new ServiceSupportInfo(service, sampleProvider,
					new SerializationFormat[] { SerializationFormat.defaultFotmat() }));
			flag = false;
		} catch (ApplicationSpecificErrorException e) {
			if (e.getErrorCode() == NameServerFunctions.ERROR_SERVICE_PROVIDER_EXISTS_CODE)
				flag = true;
		}
		Assert.assertEquals(true, flag);

	}

	@Test
	public void testGetServiceInfoById() throws Exception {
		ServiceInfo<Integer> service = new ServiceInfo<Integer>("test", 1);
		NSClient client = new NSClient(nsSPInfo, new ArrayList<>());
		boolean flag = client.register(new ServiceSupportInfo(service, sampleProvider,
				new SerializationFormat[] { SerializationFormat.defaultFotmat() }));

		Assert.assertEquals(true, flag);

		ServiceInfo<?> response = client.getServiceInfoById(1);

		Assert.assertEquals(service, response);

	}

	@Test
	public void testGetServiceInfoByName() throws Exception {
		ServiceInfo<Integer> service = new ServiceInfo<Integer>("test", 1);
		NSClient client = new NSClient(nsSPInfo, new ArrayList<>());
		boolean flag = client.register(new ServiceSupportInfo(service, sampleProvider,
				new SerializationFormat[] { SerializationFormat.defaultFotmat() }));

		Assert.assertEquals(true, flag);

		ServiceInfo<?> response = client.getServiceInfoByName("test");

		Assert.assertEquals(service, response);

	}

	//
	@Test
	public void testGetProvider() throws Exception {
		ServiceInfo<Integer> service = new ServiceInfo<Integer>("test", 1);

		NSClient client = new NSClient(nsSPInfo, new ArrayList<>());
		boolean flag = client.register(new ServiceSupportInfo(service, sampleProvider,
				new SerializationFormat[] { SerializationFormat.defaultFotmat() }));
		Assert.assertEquals(true, flag);

		ServiceSupportInfo provider = client.getProvider(service);

		Assert.assertEquals(service, provider.getService());

		Assert.assertEquals(sampleProvider, provider.getProvider());

		Assert.assertEquals(1, provider.getSerializers().length);
		Assert.assertEquals(SerializationFormat.defaultFotmat().getName(), provider.getSerializers()[0].getName());
		Assert.assertEquals(SerializationFormat.defaultFotmat().getVersion(),
				provider.getSerializers()[0].getVersion());

	 }

	@Test
	public void testGetProviderEmpty() throws Exception {
		ServiceInfo<Integer> service = new ServiceInfo<Integer>("test", 1);

		NSClient client = new NSClient(nsSPInfo, new ArrayList<>());

		ServiceSupportInfo provider = client.getProvider(service);

		Assert.assertNull(provider);
	}

	@Test
	public void testCheckProviderStatus() throws Exception {
		ServiceInfo<Integer> service = new ServiceInfo<Integer>("test", 1);

		NSClient client = new NSClient(nsSPInfo, new ArrayList<>());
		boolean flag = client.register(new ServiceSupportInfo(service, sampleProvider,
				new SerializationFormat[] { SerializationFormat.defaultFotmat() }));
		Assert.assertEquals(true, flag);

		ServiceSupportInfo provider = client.getProvider(service);

		Assert.assertEquals(service, provider.getService());

		Assert.assertEquals(sampleProvider, provider.getProvider());

		Assert.assertEquals(1, provider.getSerializers().length);
		Assert.assertEquals(SerializationFormat.defaultFotmat().getName(), provider.getSerializers()[0].getName());
		Assert.assertEquals(SerializationFormat.defaultFotmat().getVersion(),
				provider.getSerializers()[0].getVersion());

		flag = client.checkProviderStatus(sampleProvider);
		Assert.assertEquals(true, flag);

		provider = client.getProvider(service);

		Assert.assertNull(provider);

	}

	@Test
	public void testGetProvidersEmpty() throws Exception {
		ServiceInfo<Integer> service = new ServiceInfo<Integer>("test", 1);

		NSClient client = new NSClient(nsSPInfo, new ArrayList<>());

		ServiceSupportInfo[] providers = client.getProviders(service);

		Assert.assertEquals(0, providers.length);

	}

	@Test
	public void testGetAllProvidersEmpty() throws Exception {

		NSClient client = new NSClient(nsSPInfo, new ArrayList<>());

		ServiceSupportInfo[] providers = client.getAllProviders();

		Assert.assertEquals(0, providers.length);

	}

	@Test
	public void testGetServiceInfoEmpty() throws Exception {

		NSClient client = new NSClient(nsSPInfo, new ArrayList<>());
		ServiceInfo<?> service = client.getServiceInfoById(1);

		Assert.assertNull(service);

		service = client.getServiceInfoByName("test");
		Assert.assertNull(service);

	}

	@Test
	public void testGetProviders() throws Exception {
		int[] ports = new int[] { 1, 2, 3, 4 };

		ServiceInfo<Integer> service = new ServiceInfo<Integer>("test", 1);
		ServiceProviderInfo provider1 = new ServiceProviderInfo("tes1", ports[0], EnvironmentInfo.currentEnvInfo());
		ServiceProviderInfo provider2 = new ServiceProviderInfo("test2", ports[1], EnvironmentInfo.currentEnvInfo());
		ServiceProviderInfo provider3 = new ServiceProviderInfo("test3", ports[2], EnvironmentInfo.currentEnvInfo());

		ServiceInfo<Integer> service2 = new ServiceInfo<Integer>("tes2", 2);
		ServiceProviderInfo provider4 = new ServiceProviderInfo("tes1", ports[3], EnvironmentInfo.currentEnvInfo());

		NSClient client = new NSClient(nsSPInfo, new ArrayList<>());
		boolean flag = client.register(new ServiceSupportInfo(service, provider1,
				new SerializationFormat[] { SerializationFormat.defaultFotmat() }));
		boolean flag2 = client.register(new ServiceSupportInfo(service, provider2,
				new SerializationFormat[] { SerializationFormat.defaultFotmat() }));
		boolean flag3 = client.register(new ServiceSupportInfo(service, provider3,
				new SerializationFormat[] { SerializationFormat.defaultFotmat() }));
		boolean flag4 = client.register(new ServiceSupportInfo(service2, provider4,
				new SerializationFormat[] { SerializationFormat.defaultFotmat() }));

		Assert.assertEquals(true, flag & flag2 & flag3 & flag4);

		ServiceSupportInfo[] providers = client.getProviders(service);

		Assert.assertEquals(3, providers.length);

		Assert.assertEquals(service, providers[0].getService());
		Assert.assertEquals(service, providers[1].getService());
		Assert.assertEquals(service, providers[2].getService());

		Assert.assertEquals(1, providers[0].getSerializers().length);
		Assert.assertEquals(1, providers[1].getSerializers().length);
		Assert.assertEquals(1, providers[2].getSerializers().length);

		Assert.assertEquals(SerializationFormat.defaultFotmat(), providers[0].getSerializers()[0]);
		Assert.assertEquals(SerializationFormat.defaultFotmat(), providers[1].getSerializers()[0]);
		Assert.assertEquals(SerializationFormat.defaultFotmat(), providers[2].getSerializers()[0]);

		Assert.assertEquals(provider1, providers[0].getProvider());
		Assert.assertEquals(provider2, providers[1].getProvider());
		Assert.assertEquals(provider3, providers[2].getProvider());

	}

	@Test
	public void testGetAllProviders() throws Exception {
		int[] ports = new int[] { 1, 2, 3, 4 };

		ServiceInfo<Integer> service = new ServiceInfo<Integer>("test", 1);
		ServiceProviderInfo provider1 = new ServiceProviderInfo("tes1", ports[0], EnvironmentInfo.currentEnvInfo());
		ServiceProviderInfo provider2 = new ServiceProviderInfo("test2", ports[1], EnvironmentInfo.currentEnvInfo());
		ServiceProviderInfo provider3 = new ServiceProviderInfo("test3", ports[2], EnvironmentInfo.currentEnvInfo());

		ServiceInfo<Integer> service2 = new ServiceInfo<Integer>("tes2", 2);
		ServiceProviderInfo provider4 = new ServiceProviderInfo("tes1", ports[3], EnvironmentInfo.currentEnvInfo());

		NSClient client = new NSClient(nsSPInfo, new ArrayList<>());
		boolean flag = client.register(new ServiceSupportInfo(service, provider1,
				new SerializationFormat[] { SerializationFormat.defaultFotmat() }));
		boolean flag2 = client.register(new ServiceSupportInfo(service, provider2,
				new SerializationFormat[] { SerializationFormat.defaultFotmat() }));
		boolean flag3 = client.register(new ServiceSupportInfo(service, provider3,
				new SerializationFormat[] { SerializationFormat.defaultFotmat() }));
		boolean flag4 = client.register(new ServiceSupportInfo(service2, provider4,
				new SerializationFormat[] { SerializationFormat.defaultFotmat() }));

		Assert.assertEquals(true, flag & flag2 & flag3 & flag4);

		ServiceSupportInfo[] providers = client.getAllProviders();

		Assert.assertEquals(4, providers.length);

		Assert.assertEquals(service, providers[0].getService());
		Assert.assertEquals(service, providers[1].getService());
		Assert.assertEquals(service, providers[2].getService());
		Assert.assertEquals(service2, providers[3].getService());

		Assert.assertEquals(1, providers[0].getSerializers().length);
		Assert.assertEquals(1, providers[1].getSerializers().length);
		Assert.assertEquals(1, providers[2].getSerializers().length);
		Assert.assertEquals(1, providers[3].getSerializers().length);

		Assert.assertEquals(SerializationFormat.defaultFotmat(), providers[0].getSerializers()[0]);
		Assert.assertEquals(SerializationFormat.defaultFotmat(), providers[1].getSerializers()[0]);
		Assert.assertEquals(SerializationFormat.defaultFotmat(), providers[2].getSerializers()[0]);
		Assert.assertEquals(SerializationFormat.defaultFotmat(), providers[3].getSerializers()[0]);

		Assert.assertEquals(provider1, providers[0].getProvider());
		Assert.assertEquals(provider2, providers[1].getProvider());
		Assert.assertEquals(provider3, providers[2].getProvider());
		Assert.assertEquals(provider4, providers[3].getProvider());
	}

	@Test
	public void testUnregister() throws Exception {
		int[] ports = new int[] { 1, 2, 3, 4 };

		ServiceInfo<Integer> service = new ServiceInfo<Integer>("test", 1);
		ServiceProviderInfo provider1 = new ServiceProviderInfo("tes1", ports[0], EnvironmentInfo.currentEnvInfo());
		ServiceProviderInfo provider2 = new ServiceProviderInfo("test2", ports[1], EnvironmentInfo.currentEnvInfo());
		ServiceProviderInfo provider3 = new ServiceProviderInfo("test3", ports[2], EnvironmentInfo.currentEnvInfo());

		ServiceInfo<Integer> service2 = new ServiceInfo<Integer>("tes2", 2);
		ServiceProviderInfo provider4 = new ServiceProviderInfo("tes1", ports[3], EnvironmentInfo.currentEnvInfo());

		NSClient client = new NSClient(nsSPInfo, new ArrayList<>());
		boolean flag = client.register(new ServiceSupportInfo(service, provider1,
				new SerializationFormat[] { SerializationFormat.defaultFotmat() }));
		boolean flag2 = client.register(new ServiceSupportInfo(service, provider2,
				new SerializationFormat[] { SerializationFormat.defaultFotmat() }));
		boolean flag3 = client.register(new ServiceSupportInfo(service, provider3,
				new SerializationFormat[] { SerializationFormat.defaultFotmat() }));
		boolean flag4 = client.register(new ServiceSupportInfo(service2, provider4,
				new SerializationFormat[] { SerializationFormat.defaultFotmat() }));

		boolean flag5 = client.unregister(service2, provider4);

		Assert.assertEquals(true, flag & flag2 & flag3 & flag4 & flag5);

		ServiceSupportInfo[] providers = client.getAllProviders();

		Assert.assertEquals(3, providers.length);

		Assert.assertEquals(service, providers[0].getService());
		Assert.assertEquals(service, providers[1].getService());
		Assert.assertEquals(service, providers[2].getService());

		Assert.assertEquals(1, providers[0].getSerializers().length);
		Assert.assertEquals(1, providers[1].getSerializers().length);
		Assert.assertEquals(1, providers[2].getSerializers().length);

		Assert.assertEquals(SerializationFormat.defaultFotmat(), providers[0].getSerializers()[0]);
		Assert.assertEquals(SerializationFormat.defaultFotmat(), providers[1].getSerializers()[0]);
		Assert.assertEquals(SerializationFormat.defaultFotmat(), providers[2].getSerializers()[0]);

		Assert.assertEquals(provider1, providers[0].getProvider());
		Assert.assertEquals(provider2, providers[1].getProvider());
		Assert.assertEquals(provider3, providers[2].getProvider());
	}

	@Test
	public void testUnregisterAll() throws Exception {
		int[] ports = new int[] { 1, 2, 3, 4 };

		ServiceInfo<Integer> service = new ServiceInfo<Integer>("test", 1);
		ServiceProviderInfo provider1 = new ServiceProviderInfo("tes1", ports[0], EnvironmentInfo.currentEnvInfo());
		ServiceProviderInfo provider2 = new ServiceProviderInfo("test2", ports[1], EnvironmentInfo.currentEnvInfo());
		ServiceProviderInfo provider3 = new ServiceProviderInfo("test3", ports[2], EnvironmentInfo.currentEnvInfo());

		ServiceInfo<Integer> service2 = new ServiceInfo<Integer>("tes2", 2);

		NSClient client = new NSClient(nsSPInfo, new ArrayList<>());
		boolean flag = client.register(new ServiceSupportInfo(service, provider1,
				new SerializationFormat[] { SerializationFormat.defaultFotmat() }));
		boolean flag2 = client.register(new ServiceSupportInfo(service, provider2,
				new SerializationFormat[] { SerializationFormat.defaultFotmat() }));
		boolean flag3 = client.register(new ServiceSupportInfo(service, provider3,
				new SerializationFormat[] { SerializationFormat.defaultFotmat() }));
		boolean flag4 = client.register(new ServiceSupportInfo(service2, provider1,
				new SerializationFormat[] { SerializationFormat.defaultFotmat() }));

		boolean flag5 = client.unregisterAll(provider1);

		Assert.assertEquals(true, flag & flag2 & flag3 & flag4 & flag5);

		ServiceSupportInfo[] providers = client.getAllProviders();

		Assert.assertEquals(2, providers.length);

		Assert.assertEquals(service, providers[0].getService());
		Assert.assertEquals(service, providers[1].getService());

		Assert.assertEquals(1, providers[0].getSerializers().length);
		Assert.assertEquals(1, providers[1].getSerializers().length);

		Assert.assertEquals(SerializationFormat.defaultFotmat(), providers[0].getSerializers()[0]);
		Assert.assertEquals(SerializationFormat.defaultFotmat(), providers[1].getSerializers()[0]);

		Assert.assertEquals(provider2, providers[0].getProvider());
		Assert.assertEquals(provider3, providers[1].getProvider());
	}
	//
	@After
	public void stopNS() throws Exception {
		ns.stop();
		ns = null;

	}

}
