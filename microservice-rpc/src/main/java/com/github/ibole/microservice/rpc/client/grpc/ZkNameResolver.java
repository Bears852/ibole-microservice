package com.github.ibole.microservice.rpc.client.grpc;

import com.github.ibole.microservice.common.ServerIdentifier;
import com.github.ibole.microservice.discovery.DiscoveryFactory;
import com.github.ibole.microservice.discovery.HostMetadata;
import com.github.ibole.microservice.discovery.ServiceDiscovery;
import com.github.ibole.microservice.discovery.ServiceDiscoveryProvider;
import com.github.ibole.microservice.rpc.client.exception.RpcClientException;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.Attributes;
import io.grpc.NameResolver;
import io.grpc.ResolvedServerInfo;
import io.grpc.ResolvedServerInfoGroup;
import io.grpc.ResolvedServerInfoGroup.Builder;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A Zookeeper-based {@link NameResolver}.
 * 
 * <pre>
 * FORMAT WILL BE: zk://serviceContract
 *                      ---------------
 *                            |
 *                      Service Name (e.g. zk://UserService.user.service.ibole.github.com)     
 * </pre>
 * @see ZkNameResolverProvider
 * 
 * @author bwang
 *
 */
public class ZkNameResolver extends NameResolver {
  
  private static Logger LOGGER = LoggerFactory.getLogger(ZkNameResolver.class.getName());
  
  private ServiceDiscovery<HostMetadata> discovery = null;
  private final URI targetUri;
  private final String zoneToPrefer;
  private final boolean usedTls;
  private final Attributes params;
  
  /**
   * @param targetUri the target service Uri
   * @param params the additional parameters
   * @param zookeeperAddress 
   * @param zoneToPrefer the preferred host zone
   * @param usedTls if the tls is enable
   */
  public ZkNameResolver(URI targetUri, Attributes params, ServerIdentifier zookeeperAddress,
      String zoneToPrefer, boolean usedTls) {
    
    // Following just doing the check for the first authority.
    String targetPath = Preconditions.checkNotNull(targetUri.getPath(), "targetPath");
    Preconditions.checkArgument(targetPath.startsWith("/"),    
        "the path component (%s) of the target (%s) must start with '/'", targetPath, targetUri);

    this.targetUri = targetUri;
    this.params = params;
    this.zoneToPrefer = zoneToPrefer;
    this.usedTls = usedTls;
    
    DiscoveryFactory<ServiceDiscovery<HostMetadata>> factory =
        ServiceDiscoveryProvider.provider().getDiscoveryFactory();
    this.discovery = factory.getServiceDiscovery(zookeeperAddress);
  }

  /*
   * (non-Javadoc)
   * 
   * @see io.grpc.NameResolver#getServiceAuthority()
   */
  @Override
  public String getServiceAuthority() {

    return targetUri.getAuthority();
  }

  @Override
  public final synchronized void start(Listener listener) {
    String serviceName = new StringBuilder(targetUri.getAuthority()).reverse().toString();
    List<HostMetadata> hostList = discovery.listAll(serviceName);
    if (hostList == null || hostList.isEmpty()) {
      LOGGER.error("No services are registered for '{}' in registry center '{}'!", serviceName,
          discovery.getIdentifier());
      throw new RpcClientException("No services found!");
    }
   
    List<ResolvedServerInfoGroup> resolvedServers;
    Predicate<HostMetadata> predicateWithZoneAndTls = host -> zoneToPrefer.equalsIgnoreCase(host.getZone()) && usedTls == host.isUseTls();
    Predicate<HostMetadata> predicateWithTls = host -> usedTls == host.isUseTls();
    // Find the service servers with the same preference zone.
    resolvedServers = filterResolvedServers(hostList, predicateWithZoneAndTls);
    // Find the service servers without preference zone filtering if no preference service server found.
    if (resolvedServers.isEmpty()) {
      resolvedServers = filterResolvedServers(hostList, predicateWithTls);
    }

    listener.onUpdate(resolvedServers, params);
    
    //watch service node changes and fire the even
    discovery.watchForUpdates(serviceName, updatedList -> {
      List<ResolvedServerInfoGroup> updatedServers = filterResolvedServers(updatedList, predicateWithZoneAndTls);
      if(updatedServers.isEmpty()){
        updatedServers = filterResolvedServers(updatedList, predicateWithTls);
      }
        listener.onUpdate(updatedServers, Attributes.EMPTY);
    });

    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("ZkNameResolver is start.");
    }
  }
  
  private List<ResolvedServerInfoGroup> filterResolvedServers(List<HostMetadata> newList, Predicate<HostMetadata> predicate) {
    return newList.stream().filter(predicate).map( hostandZone -> {
        InetAddress[] allByName;
        try {
             allByName = InetAddress.getAllByName(hostandZone.getHostname());
             Builder builder = ResolvedServerInfoGroup.builder();
             Stream.of(allByName).forEach( inetAddress -> {
                   builder.add(new ResolvedServerInfo(new InetSocketAddress(inetAddress, hostandZone.getPort())));
             });
             return builder.build();
            } catch (Exception e) {
               throw Throwables.propagate(e);
            }
     }).collect(Collectors.toList());
  }

  @Override
  public final synchronized void shutdown() {
    try {
      discovery.destroy();
    } catch (Exception ex) {
      LOGGER.error("ZkNameResolver shutdown error happened", ex);
      throw new RpcClientException(ex);
    }
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("ZkNameResolver is shutdown.");
    }
  }
  
}
