/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.configuration;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.annotation.docs.Documentation;
import org.apache.flink.configuration.description.Description;
import org.apache.flink.configuration.description.TextElement;

import java.time.Duration;
import java.util.Map;

import static org.apache.flink.configuration.ConfigOptions.key;
import static org.apache.flink.configuration.description.LinkElement.link;
import static org.apache.flink.configuration.description.TextElement.text;

/** The set of configuration options relating to high-availability settings. */
@PublicEvolving
public class HighAvailabilityOptions {

    // ------------------------------------------------------------------------
    //  Required High Availability Options
    // ------------------------------------------------------------------------

    /**
     * Defines high-availability mode used for the cluster execution. A value of "NONE" signals no
     * highly available setup. To enable high-availability, set this mode to "ZOOKEEPER" or
     * "KUBERNETES". Can also be set to the FQN of the HighAvailability factory class.
     */
    @Documentation.Section(value = Documentation.Sections.COMMON_HIGH_AVAILABILITY, position = 1)
    public static final ConfigOption<String> HA_MODE =
            key("high-availability.type")
                    .stringType()
                    .defaultValue("NONE")
                    .withDeprecatedKeys("recovery.mode", "high-availability")
                    .withDescription(
                            "Defines high-availability mode used for cluster execution."
                                    + " To enable high-availability, set this mode to \"ZOOKEEPER\", \"KUBERNETES\", or specify the fully qualified name of the factory class.");

    /**
     * The ID of the Flink cluster, used to separate multiple Flink clusters Needs to be set for
     * standalone clusters, is automatically inferred in YARN.
     */
    @Documentation.Section(Documentation.Sections.COMMON_HIGH_AVAILABILITY)
    public static final ConfigOption<String> HA_CLUSTER_ID =
            key("high-availability.cluster-id")
                    .stringType()
                    .defaultValue("/default")
                    .withDeprecatedKeys(
                            "high-availability.zookeeper.path.namespace",
                            "recovery.zookeeper.path.namespace")
                    .withDescription(
                            "The ID of the Flink cluster, used to separate multiple Flink clusters from each other."
                                    + " Needs to be set for standalone clusters but is automatically inferred in YARN.");

    /** File system path (URI) where Flink persists metadata in high-availability setups. */
    @Documentation.Section(Documentation.Sections.COMMON_HIGH_AVAILABILITY)
    public static final ConfigOption<String> HA_STORAGE_PATH =
            key("high-availability.storageDir")
                    .stringType()
                    .noDefaultValue()
                    .withDeprecatedKeys(
                            "high-availability.zookeeper.storageDir",
                            "recovery.zookeeper.storageDir")
                    .withDescription(
                            "File system path (URI) where Flink persists metadata in high-availability setups.");

    // ------------------------------------------------------------------------
    //  Recovery Options
    // ------------------------------------------------------------------------

    /** Optional port (range) used by the job manager in high-availability mode. */
    @Documentation.Section(Documentation.Sections.EXPERT_HIGH_AVAILABILITY)
    public static final ConfigOption<String> HA_JOB_MANAGER_PORT_RANGE =
            key("high-availability.jobmanager.port")
                    .stringType()
                    .defaultValue("0")
                    .withDeprecatedKeys("recovery.jobmanager.port")
                    .withDescription(
                            "The port (range) used by the Flink Master for its RPC connections in highly-available setups. "
                                    + "In highly-available setups, this value is used instead of '"
                                    + JobManagerOptions.PORT.key()
                                    + "'."
                                    + "A value of '0' means that a random free port is chosen. TaskManagers discover this port through "
                                    + "the high-availability services (leader election), so a random port or a port range works "
                                    + "without requiring any additional means of service discovery.");

    // ------------------------------------------------------------------------
    //  ZooKeeper Options
    // ------------------------------------------------------------------------

    /**
     * The ZooKeeper quorum to use, when running Flink in a high-availability mode with ZooKeeper.
     */
    @Documentation.Section(Documentation.Sections.COMMON_HIGH_AVAILABILITY_ZOOKEEPER)
    public static final ConfigOption<String> HA_ZOOKEEPER_QUORUM =
            key("high-availability.zookeeper.quorum")
                    .stringType()
                    .noDefaultValue()
                    .withDeprecatedKeys("recovery.zookeeper.quorum")
                    .withDescription(
                            "The ZooKeeper quorum to use, when running Flink in a high-availability mode with ZooKeeper.");

    /** The root path under which Flink stores its entries in ZooKeeper. */
    @Documentation.Section(Documentation.Sections.COMMON_HIGH_AVAILABILITY_ZOOKEEPER)
    public static final ConfigOption<String> HA_ZOOKEEPER_ROOT =
            key("high-availability.zookeeper.path.root")
                    .stringType()
                    .defaultValue("/flink")
                    .withDeprecatedKeys("recovery.zookeeper.path.root")
                    .withDescription(
                            "The root path under which Flink stores its entries in ZooKeeper.");

    /** ZooKeeper root path (ZNode) for execution plans. */
    @Documentation.Section(Documentation.Sections.EXPERT_ZOOKEEPER_HIGH_AVAILABILITY)
    public static final ConfigOption<String> HA_ZOOKEEPER_EXECUTION_PLANS_PATH =
            key("high-availability.zookeeper.path.execution-plans")
                    .stringType()
                    .defaultValue("/execution-plans")
                    .withDeprecatedKeys(
                            "recovery.zookeeper.path.jobgraphs",
                            "high-availability.zookeeper.path.jobgraphs")
                    .withDescription("ZooKeeper root path (ZNode) for execution plans");

    // ------------------------------------------------------------------------
    //  ZooKeeper Client Settings
    // ------------------------------------------------------------------------

    @Documentation.Section(Documentation.Sections.EXPERT_ZOOKEEPER_HIGH_AVAILABILITY)
    public static final ConfigOption<Duration> ZOOKEEPER_SESSION_TIMEOUT =
            key("high-availability.zookeeper.client.session-timeout")
                    .durationType()
                    .defaultValue(Duration.ofMillis(60000))
                    .withDeprecatedKeys("recovery.zookeeper.client.session-timeout")
                    .withDescription("Defines the session timeout for the ZooKeeper session.");

    @Documentation.Section(Documentation.Sections.EXPERT_ZOOKEEPER_HIGH_AVAILABILITY)
    public static final ConfigOption<Duration> ZOOKEEPER_CONNECTION_TIMEOUT =
            key("high-availability.zookeeper.client.connection-timeout")
                    .durationType()
                    .defaultValue(Duration.ofMillis(15000))
                    .withDeprecatedKeys("recovery.zookeeper.client.connection-timeout")
                    .withDescription("Defines the connection timeout for ZooKeeper.");

    @Documentation.Section(Documentation.Sections.EXPERT_ZOOKEEPER_HIGH_AVAILABILITY)
    public static final ConfigOption<Duration> ZOOKEEPER_RETRY_WAIT =
            key("high-availability.zookeeper.client.retry-wait")
                    .durationType()
                    .defaultValue(Duration.ofMillis(5000))
                    .withDeprecatedKeys("recovery.zookeeper.client.retry-wait")
                    .withDescription("Defines the pause between consecutive retries.");

    @Documentation.Section(Documentation.Sections.EXPERT_ZOOKEEPER_HIGH_AVAILABILITY)
    public static final ConfigOption<Integer> ZOOKEEPER_MAX_RETRY_ATTEMPTS =
            key("high-availability.zookeeper.client.max-retry-attempts")
                    .intType()
                    .defaultValue(3)
                    .withDeprecatedKeys("recovery.zookeeper.client.max-retry-attempts")
                    .withDescription(
                            "Defines the number of connection retries before the client gives up.");

    @Documentation.Section(Documentation.Sections.EXPERT_ZOOKEEPER_HIGH_AVAILABILITY)
    public static final ConfigOption<String> ZOOKEEPER_CLIENT_ACL =
            key("high-availability.zookeeper.client.acl")
                    .stringType()
                    .defaultValue("open")
                    .withDescription(
                            "Defines the ACL (open|creator) to be configured on ZK node. The configuration value can be"
                                    + " set to “creator” if the ZooKeeper server configuration has the “authProvider” property mapped to use"
                                    + " SASLAuthenticationProvider and the cluster is configured to run in secure mode (Kerberos).");

    @Documentation.Section(Documentation.Sections.EXPERT_ZOOKEEPER_HIGH_AVAILABILITY)
    public static final ConfigOption<Boolean> ZOOKEEPER_TOLERATE_SUSPENDED_CONNECTIONS =
            key("high-availability.zookeeper.client.tolerate-suspended-connections")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription(
                            Description.builder()
                                    .text(
                                            "Defines whether a suspended ZooKeeper connection will be treated as an error that causes the leader "
                                                    + "information to be invalidated or not. In case you set this option to %s, Flink will wait until a "
                                                    + "ZooKeeper connection is marked as lost before it revokes the leadership of components. This has the "
                                                    + "effect that Flink is more resilient against temporary connection instabilities at the cost of running "
                                                    + "more likely into timing issues with ZooKeeper.",
                                            TextElement.code("true"))
                                    .build());

    @Documentation.Section(Documentation.Sections.EXPERT_ZOOKEEPER_HIGH_AVAILABILITY)
    public static final ConfigOption<Boolean> ZOOKEEPER_ENSEMBLE_TRACKING =
            key("high-availability.zookeeper.client.ensemble-tracker")
                    .booleanType()
                    .defaultValue(true)
                    .withDescription(
                            Description.builder()
                                    .text(
                                            "Defines whether Curator should enable ensemble tracker. This can be useful in certain scenarios "
                                                    + "in which CuratorFramework is accessing to ZK clusters via load balancer or Virtual IPs. "
                                                    + "Default Curator EnsembleTracking logic watches CuratorEventType.GET_CONFIG events and "
                                                    + "changes ZooKeeper connection string. It is not desired behaviour when ZooKeeper is running under the Virtual IPs. "
                                                    + "Under certain configurations EnsembleTracking can lead to setting of ZooKeeper connection string "
                                                    + "with unresolvable hostnames.")
                                    .build());

    public static final ConfigOption<Map<String, String>> ZOOKEEPER_CLIENT_AUTHORIZATION =
            key("high-availability.zookeeper.client.authorization")
                    .mapType()
                    .noDefaultValue()
                    .withDescription(
                            Description.builder()
                                    .text(
                                            "Add connection authorization Subsequent calls to this method overwrite the prior calls. "
                                                    + "In certain cases ZooKeeper requires additional Authorization information. "
                                                    + "For example list of valid names for ensemble in order to prevent accidentally connecting to a wrong ensemble. "
                                                    + "Each entry of type Map.Entry<String, String> will be transformed "
                                                    + "into an AuthInfo object with the constructor AuthInfo(String, byte[]). "
                                                    + "The field entry.key() will serve as the String scheme value, while the field entry.getValue() "
                                                    + "will be initially converted to a byte[] using the String#getBytes() method with %s encoding. "
                                                    + "If not set the default configuration for a Curator would be applied.",
                                            text(ConfigConstants.DEFAULT_CHARSET.displayName()))
                                    .build());

    public static final ConfigOption<Duration> ZOOKEEPER_MAX_CLOSE_WAIT =
            key("high-availability.zookeeper.client.max-close-wait")
                    .durationType()
                    .noDefaultValue()
                    .withDescription(
                            Description.builder()
                                    .text(
                                            "Defines the time Curator should wait during close to join background threads. "
                                                    + "If not set the default configuration for a Curator would be applied.")
                                    .build());

    public static final ConfigOption<Integer> ZOOKEEPER_SIMULATED_SESSION_EXP_PERCENT =
            key("high-availability.zookeeper.client.simulated-session-expiration-percent")
                    .intType()
                    .noDefaultValue()
                    .withDescription(
                            Description.builder()
                                    .text(
                                            "The percentage set by this method determines how and if Curator will check for session expiration. "
                                                    + "See Curator documentation for %s property for more information.",
                                            link(
                                                    "https://curator.apache.org/apidocs/org/apache/curator/framework/"
                                                            + "CuratorFrameworkFactory.Builder.html#simulatedSessionExpirationPercent(int)",
                                                    "simulatedSessionExpirationPercent"))
                                    .build());

    // ------------------------------------------------------------------------

    /** Not intended to be instantiated. */
    private HighAvailabilityOptions() {}
}
