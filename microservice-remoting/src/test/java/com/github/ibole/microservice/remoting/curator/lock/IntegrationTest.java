package com.github.ibole.microservice.remoting.curator.lock;

import com.github.ibole.microservice.remoting.DistributedLockService;
import com.github.ibole.microservice.remoting.curator.lock.CuratorDistributedLockServiceProvider;

import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ZooKeeperServerMain;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

public class IntegrationTest {

	private CuratorDistributedLockServiceProvider provider;

	private ZooKeeperServerMain zooKeeperServer;
	
	@Before
	public void initialize() {
		
		Properties startupProperties = new Properties();
		
		startupProperties.put("dataDir", "d:/tmp/zookeeper");
		startupProperties.put("clientPort", "12181");

		
		QuorumPeerConfig quorumConfiguration = new QuorumPeerConfig();
		try {
		    quorumConfiguration.parseProperties(startupProperties);
		} catch(Exception e) {
		    throw new RuntimeException(e);
		}

		zooKeeperServer = new ZooKeeperServerMain();
		final ServerConfig configuration = new ServerConfig();
		configuration.readFrom(quorumConfiguration);

		new Thread() {
		    public void run() {
		        try {
		            zooKeeperServer.runFromConfig(configuration);
		        } catch (IOException e) {
		        	e.printStackTrace();
		        }
		    }
		}.start();
		
		
		provider = new CuratorDistributedLockServiceProvider("localhost:12181", "1000", "1", "/test");
	}
	
	@Test
	public void lock() {
		final String lockName = UUID.randomUUID().toString();
		
		DistributedLockService lock = provider.getDistributedLock(1000);
		Assert.assertTrue(lock.tryLock(lockName));
		Assert.assertTrue(lock.tryLock(lockName));
		DistributedLockService lock2 = provider.getDistributedLock(1000);
		Assert.assertFalse(lock2.tryLock(lockName));
	}

}