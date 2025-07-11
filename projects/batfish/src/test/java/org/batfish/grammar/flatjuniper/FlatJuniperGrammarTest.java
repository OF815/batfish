package org.batfish.grammar.flatjuniper;

import static com.google.common.collect.Iterables.getOnlyElement;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.batfish.common.matchers.ParseWarningMatchers.hasComment;
import static org.batfish.common.matchers.ParseWarningMatchers.hasText;
import static org.batfish.common.util.Resources.readResource;
import static org.batfish.datamodel.AbstractRoute.MAX_TAG;
import static org.batfish.datamodel.AuthenticationMethod.GROUP_RADIUS;
import static org.batfish.datamodel.AuthenticationMethod.GROUP_TACACS;
import static org.batfish.datamodel.AuthenticationMethod.PASSWORD;
import static org.batfish.datamodel.BgpRoute.MAX_LOCAL_PREFERENCE;
import static org.batfish.datamodel.Configuration.DEFAULT_VRF_NAME;
import static org.batfish.datamodel.Flow.builder;
import static org.batfish.datamodel.Ip.ZERO;
import static org.batfish.datamodel.IpProtocol.ICMP;
import static org.batfish.datamodel.IpProtocol.OSPF;
import static org.batfish.datamodel.IpProtocol.UDP;
import static org.batfish.datamodel.Names.zoneToZoneFilter;
import static org.batfish.datamodel.OriginMechanism.LEARNED;
import static org.batfish.datamodel.Route.UNSET_ROUTE_NEXT_HOP_IP;
import static org.batfish.datamodel.acl.AclLineMatchExprs.and;
import static org.batfish.datamodel.acl.AclLineMatchExprs.match;
import static org.batfish.datamodel.acl.AclLineMatchExprs.matchDst;
import static org.batfish.datamodel.acl.AclLineMatchExprs.matchIcmpCode;
import static org.batfish.datamodel.acl.AclLineMatchExprs.matchIcmpType;
import static org.batfish.datamodel.acl.AclLineMatchExprs.matchIpProtocol;
import static org.batfish.datamodel.acl.AclLineMatchExprs.matchSrc;
import static org.batfish.datamodel.acl.AclLineMatchExprs.matchSrcInterface;
import static org.batfish.datamodel.acl.AclLineMatchExprs.or;
import static org.batfish.datamodel.flow.TransformationStep.TransformationType.DEST_NAT;
import static org.batfish.datamodel.flow.TransformationStep.TransformationType.SOURCE_NAT;
import static org.batfish.datamodel.flow.TransformationStep.TransformationType.STATIC_NAT;
import static org.batfish.datamodel.matchers.AaaAuthenticationLoginListMatchers.hasMethods;
import static org.batfish.datamodel.matchers.AbstractRouteDecoratorMatchers.hasAdministrativeCost;
import static org.batfish.datamodel.matchers.AbstractRouteDecoratorMatchers.hasNextHop;
import static org.batfish.datamodel.matchers.AbstractRouteDecoratorMatchers.hasPrefix;
import static org.batfish.datamodel.matchers.AbstractRouteDecoratorMatchers.hasProtocol;
import static org.batfish.datamodel.matchers.AbstractRouteDecoratorMatchers.hasTag;
import static org.batfish.datamodel.matchers.AddressFamilyCapabilitiesMatchers.hasAllowLocalAsIn;
import static org.batfish.datamodel.matchers.AddressFamilyMatchers.hasAddressFamilyCapabilites;
import static org.batfish.datamodel.matchers.AnnotatedRouteMatchers.hasSourceVrf;
import static org.batfish.datamodel.matchers.BgpNeighborMatchers.hasClusterId;
import static org.batfish.datamodel.matchers.BgpNeighborMatchers.hasEnforceFirstAs;
import static org.batfish.datamodel.matchers.BgpNeighborMatchers.hasIpv4UnicastAddressFamily;
import static org.batfish.datamodel.matchers.BgpNeighborMatchers.hasLocalAs;
import static org.batfish.datamodel.matchers.BgpNeighborMatchers.hasRemoteAs;
import static org.batfish.datamodel.matchers.BgpProcessMatchers.hasActiveNeighbor;
import static org.batfish.datamodel.matchers.BgpProcessMatchers.hasMultipathEbgp;
import static org.batfish.datamodel.matchers.BgpProcessMatchers.hasMultipathIbgp;
import static org.batfish.datamodel.matchers.BgpProcessMatchers.hasNeighbors;
import static org.batfish.datamodel.matchers.BgpProcessMatchers.hasPassiveNeighbor;
import static org.batfish.datamodel.matchers.BgpRouteMatchers.hasCommunities;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasDefaultVrf;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasHostname;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasIkePhase1Policy;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasIkePhase1Proposal;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasInterface;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasInterfaces;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasIpAccessList;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasIpsecPeerConfig;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasIpsecPhase2Policy;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasIpsecPhase2Proposal;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasVrf;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasVrfs;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasBandwidth;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasDefinedStructure;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasDefinedStructureWithDefinitionLines;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasIncomingFilter;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasIsisProcess;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasNoUndefinedReferences;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasNumReferrers;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasParseWarning;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasRedFlagWarning;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasReferenceBandwidth;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasReferencedStructure;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasRouteFilterList;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasRouteFilterLists;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasUndefinedReference;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasUndefinedReferenceWithReferenceLines;
import static org.batfish.datamodel.matchers.IkePhase1PolicyMatchers.hasIkePhase1Key;
import static org.batfish.datamodel.matchers.IkePhase1PolicyMatchers.hasIkePhase1Proposals;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasAccessVlan;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasAdditionalArpIps;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasAllAddresses;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasAllowedVlans;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasDescription;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasEncapsulationVlan;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasInterfaceType;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasIsis;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasMtu;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasNativeVlan;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasOspfAreaName;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasOspfCost;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasOspfEnabled;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasOspfNetworkType;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasSwitchPortMode;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasVlan;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasZoneName;
import static org.batfish.datamodel.matchers.InterfaceMatchers.isActive;
import static org.batfish.datamodel.matchers.InterfaceMatchers.isOspfPassive;
import static org.batfish.datamodel.matchers.InterfaceMatchers.isSwitchport;
import static org.batfish.datamodel.matchers.IpAccessListMatchers.accepts;
import static org.batfish.datamodel.matchers.IpAccessListMatchers.hasName;
import static org.batfish.datamodel.matchers.IpAccessListMatchers.rejects;
import static org.batfish.datamodel.matchers.IpSpaceMatchers.containsIp;
import static org.batfish.datamodel.matchers.IpsecPeerConfigMatchers.hasDestinationAddress;
import static org.batfish.datamodel.matchers.IpsecPeerConfigMatchers.isIpsecStaticPeerConfigThat;
import static org.batfish.datamodel.matchers.IsisInterfaceLevelSettingsMatchers.hasCost;
import static org.batfish.datamodel.matchers.IsisInterfaceLevelSettingsMatchers.hasHelloAuthenticationType;
import static org.batfish.datamodel.matchers.IsisInterfaceLevelSettingsMatchers.hasHoldTime;
import static org.batfish.datamodel.matchers.IsisInterfaceLevelSettingsMatchers.hasMode;
import static org.batfish.datamodel.matchers.IsisInterfaceSettingsMatchers.hasBfdLivenessDetectionMinimumInterval;
import static org.batfish.datamodel.matchers.IsisInterfaceSettingsMatchers.hasBfdLivenessDetectionMultiplier;
import static org.batfish.datamodel.matchers.IsisInterfaceSettingsMatchers.hasIsoAddress;
import static org.batfish.datamodel.matchers.IsisInterfaceSettingsMatchers.hasLevel1;
import static org.batfish.datamodel.matchers.IsisInterfaceSettingsMatchers.hasLevel2;
import static org.batfish.datamodel.matchers.IsisLevelSettingsMatchers.hasWideMetricsOnly;
import static org.batfish.datamodel.matchers.IsisProcessMatchers.hasNetAddress;
import static org.batfish.datamodel.matchers.IsisProcessMatchers.hasOverload;
import static org.batfish.datamodel.matchers.LineMatchers.hasAuthenticationLoginList;
import static org.batfish.datamodel.matchers.LiteralIntMatcher.hasVal;
import static org.batfish.datamodel.matchers.LiteralIntMatcher.isLiteralIntThat;
import static org.batfish.datamodel.matchers.MapMatchers.hasKeys;
import static org.batfish.datamodel.matchers.NssaSettingsMatchers.hasDefaultOriginateType;
import static org.batfish.datamodel.matchers.NssaSettingsMatchers.hasSuppressType3;
import static org.batfish.datamodel.matchers.OspfAreaMatchers.hasInjectDefaultRoute;
import static org.batfish.datamodel.matchers.OspfAreaMatchers.hasMetricOfDefaultRoute;
import static org.batfish.datamodel.matchers.OspfAreaMatchers.hasNssa;
import static org.batfish.datamodel.matchers.OspfAreaMatchers.hasStub;
import static org.batfish.datamodel.matchers.OspfAreaMatchers.hasStubType;
import static org.batfish.datamodel.matchers.OspfAreaMatchers.hasSummary;
import static org.batfish.datamodel.matchers.OspfAreaSummaryMatchers.hasMetric;
import static org.batfish.datamodel.matchers.OspfAreaSummaryMatchers.installsDiscard;
import static org.batfish.datamodel.matchers.OspfAreaSummaryMatchers.isAdvertised;
import static org.batfish.datamodel.matchers.OspfProcessMatchers.hasArea;
import static org.batfish.datamodel.matchers.OspfProcessMatchers.hasRouterId;
import static org.batfish.datamodel.matchers.RouteFilterListMatchers.permits;
import static org.batfish.datamodel.matchers.SetAdministrativeCostMatchers.hasAdmin;
import static org.batfish.datamodel.matchers.SetAdministrativeCostMatchers.isSetAdministrativeCostThat;
import static org.batfish.datamodel.matchers.VrfMatchers.hasBgpProcess;
import static org.batfish.datamodel.matchers.VrfMatchers.hasOspfProcess;
import static org.batfish.datamodel.matchers.VrfMatchers.hasStaticRoutes;
import static org.batfish.datamodel.routing_policy.Environment.Direction.IN;
import static org.batfish.datamodel.transformation.IpField.DESTINATION;
import static org.batfish.datamodel.transformation.IpField.SOURCE;
import static org.batfish.datamodel.transformation.Noop.NOOP_DEST_NAT;
import static org.batfish.datamodel.transformation.Transformation.when;
import static org.batfish.datamodel.transformation.TransformationStep.assignDestinationIp;
import static org.batfish.datamodel.transformation.TransformationStep.assignSourceIp;
import static org.batfish.datamodel.transformation.TransformationStep.assignSourcePort;
import static org.batfish.datamodel.vendor_family.juniper.JuniperFamily.AUXILIARY_LINE_NAME;
import static org.batfish.datamodel.vendor_family.juniper.JuniperFamily.CONSOLE_LINE_NAME;
import static org.batfish.main.BatfishTestUtils.DUMMY_SNAPSHOT_1;
import static org.batfish.representation.juniper.JuniperConfiguration.ACL_NAME_GLOBAL_POLICY;
import static org.batfish.representation.juniper.JuniperConfiguration.ACL_NAME_SECURITY_POLICY;
import static org.batfish.representation.juniper.JuniperConfiguration.DEFAULT_ISIS_COST;
import static org.batfish.representation.juniper.JuniperConfiguration.computeConditionTrackName;
import static org.batfish.representation.juniper.JuniperConfiguration.computeOspfExportPolicyName;
import static org.batfish.representation.juniper.JuniperConfiguration.computePeerExportPolicyName;
import static org.batfish.representation.juniper.JuniperConfiguration.computePolicyStatementTermName;
import static org.batfish.representation.juniper.JuniperConfiguration.computeSecurityPolicyTermName;
import static org.batfish.representation.juniper.JuniperConfiguration.firewallFilterTermVendorStructureId;
import static org.batfish.representation.juniper.JuniperConfiguration.generateInstanceImportPolicyName;
import static org.batfish.representation.juniper.JuniperConfiguration.generateResolutionRibImportPolicyName;
import static org.batfish.representation.juniper.JuniperConfiguration.matchingFirewallFilterTerm;
import static org.batfish.representation.juniper.JuniperConfiguration.matchingSecurityPolicyTerm;
import static org.batfish.representation.juniper.JuniperConfiguration.securityPolicyTermVendorStructureId;
import static org.batfish.representation.juniper.JuniperStructureType.APPLICATION;
import static org.batfish.representation.juniper.JuniperStructureType.APPLICATION_OR_APPLICATION_SET;
import static org.batfish.representation.juniper.JuniperStructureType.APPLICATION_SET;
import static org.batfish.representation.juniper.JuniperStructureType.AUTHENTICATION_KEY_CHAIN;
import static org.batfish.representation.juniper.JuniperStructureType.BGP_GROUP;
import static org.batfish.representation.juniper.JuniperStructureType.BGP_NEIGHBOR;
import static org.batfish.representation.juniper.JuniperStructureType.BRIDGE_DOMAIN;
import static org.batfish.representation.juniper.JuniperStructureType.CLASS_OF_SERVICE_CODE_POINT_ALIAS;
import static org.batfish.representation.juniper.JuniperStructureType.COMMUNITY;
import static org.batfish.representation.juniper.JuniperStructureType.FIREWALL_FILTER;
import static org.batfish.representation.juniper.JuniperStructureType.FIREWALL_FILTER_TERM;
import static org.batfish.representation.juniper.JuniperStructureType.FIREWALL_INTERFACE_SET;
import static org.batfish.representation.juniper.JuniperStructureType.INTERFACE;
import static org.batfish.representation.juniper.JuniperStructureType.POLICY_STATEMENT;
import static org.batfish.representation.juniper.JuniperStructureType.POLICY_STATEMENT_TERM;
import static org.batfish.representation.juniper.JuniperStructureType.PREFIX_LIST;
import static org.batfish.representation.juniper.JuniperStructureType.RTF_PREFIX_LIST;
import static org.batfish.representation.juniper.JuniperStructureType.SECURITY_POLICY_TERM;
import static org.batfish.representation.juniper.JuniperStructureType.SRLG;
import static org.batfish.representation.juniper.JuniperStructureType.TUNNEL_ATTRIBUTE;
import static org.batfish.representation.juniper.JuniperStructureType.VLAN;
import static org.batfish.representation.juniper.JuniperStructureUsage.APPLICATION_SET_MEMBER_APPLICATION;
import static org.batfish.representation.juniper.JuniperStructureUsage.APPLICATION_SET_MEMBER_APPLICATION_SET;
import static org.batfish.representation.juniper.JuniperStructureUsage.INTERFACE_VLAN;
import static org.batfish.representation.juniper.JuniperStructureUsage.MPLS_INTERFACE_SRLG;
import static org.batfish.representation.juniper.JuniperStructureUsage.OSPF_AREA_INTERFACE;
import static org.batfish.representation.juniper.JuniperStructureUsage.POLICY_STATEMENT_FROM_COMMUNITY;
import static org.batfish.representation.juniper.JuniperStructureUsage.POLICY_STATEMENT_RTF_PREFIX_LIST;
import static org.batfish.representation.juniper.JuniperStructureUsage.POLICY_STATEMENT_THEN_TUNNEL_ATTRIBUTE;
import static org.batfish.representation.juniper.JuniperStructureUsage.SECURITY_POLICY_MATCH_APPLICATION;
import static org.batfish.representation.juniper.RoutingInformationBase.RIB_IPV4_UNICAST;
import static org.batfish.representation.juniper.RoutingInformationBase.RIB_IPV6_UNICAST;
import static org.batfish.representation.juniper.RoutingInstance.OSPF_INTERNAL_SUMMARY_DISCARD_METRIC;
import static org.batfish.representation.juniper.Zone.getInboundFilterName;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasValue;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Range;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.lang3.SerializationUtils;
import org.batfish.common.BatfishLogger;
import org.batfish.common.Warning;
import org.batfish.common.Warnings;
import org.batfish.common.Warnings.ParseWarning;
import org.batfish.common.matchers.WarningMatchers;
import org.batfish.common.matchers.WarningsMatchers;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.topology.L3Adjacencies;
import org.batfish.common.topology.Layer1Edge;
import org.batfish.common.topology.Layer1Topology;
import org.batfish.common.util.CommonUtil;
import org.batfish.config.Settings;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.AclAclLine;
import org.batfish.datamodel.AclIpSpace;
import org.batfish.datamodel.AclLine;
import org.batfish.datamodel.AnnotatedRoute;
import org.batfish.datamodel.AsPath;
import org.batfish.datamodel.BddTestbed;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpPeerConfig;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.BgpSessionProperties;
import org.batfish.datamodel.Bgpv4Route;
import org.batfish.datamodel.Bgpv4Route.Builder;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.ConnectedRoute;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.DiffieHellmanGroup;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.EncryptionAlgorithm;
import org.batfish.datamodel.ExprAclLine;
import org.batfish.datamodel.FirewallSessionInterfaceInfo;
import org.batfish.datamodel.FirewallSessionInterfaceInfo.Action;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.GeneratedRoute;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.IkeAuthenticationMethod;
import org.batfish.datamodel.IkeHashingAlgorithm;
import org.batfish.datamodel.IntegerSpace;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Interface.Dependency;
import org.batfish.datamodel.Interface.DependencyType;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Ip6;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.IpSpaceReference;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.IpsecAuthenticationAlgorithm;
import org.batfish.datamodel.IpsecEncapsulationMode;
import org.batfish.datamodel.IpsecProtocol;
import org.batfish.datamodel.IsisRoute;
import org.batfish.datamodel.IsoAddress;
import org.batfish.datamodel.Line;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.LocalRoute;
import org.batfish.datamodel.MainRibVrfLeakConfig;
import org.batfish.datamodel.MultipathEquivalentAsPathMatchMode;
import org.batfish.datamodel.NamedPort;
import org.batfish.datamodel.Names;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.OspfExternalType1Route;
import org.batfish.datamodel.OspfExternalType2Route;
import org.batfish.datamodel.OspfInterAreaRoute;
import org.batfish.datamodel.OspfIntraAreaRoute;
import org.batfish.datamodel.OspfRoute;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Prefix6;
import org.batfish.datamodel.ReceivedFromIp;
import org.batfish.datamodel.RouteFilterLine;
import org.batfish.datamodel.RouteFilterList;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.SnmpCommunity;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.SwitchportMode;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.TraceElement;
import org.batfish.datamodel.UseConstantIp;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.AclLineMatchExprs;
import org.batfish.datamodel.acl.AndMatchExpr;
import org.batfish.datamodel.acl.MatchHeaderSpace;
import org.batfish.datamodel.acl.OrMatchExpr;
import org.batfish.datamodel.acl.PermittedByAcl;
import org.batfish.datamodel.answers.ConvertConfigurationAnswerElement;
import org.batfish.datamodel.answers.InitInfoAnswerElement;
import org.batfish.datamodel.answers.ParseVendorConfigurationAnswerElement;
import org.batfish.datamodel.bgp.AddressFamily;
import org.batfish.datamodel.bgp.AddressFamilyCapabilities;
import org.batfish.datamodel.bgp.BgpConfederation;
import org.batfish.datamodel.bgp.RouteDistinguisher;
import org.batfish.datamodel.bgp.community.StandardCommunity;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.isis.IsisHelloAuthenticationType;
import org.batfish.datamodel.isis.IsisInterfaceMode;
import org.batfish.datamodel.isis.IsisInterfaceSettings;
import org.batfish.datamodel.isis.IsisLevel;
import org.batfish.datamodel.isis.IsisProcess;
import org.batfish.datamodel.matchers.AbstractRouteDecoratorMatchers;
import org.batfish.datamodel.matchers.IkePhase1KeyMatchers;
import org.batfish.datamodel.matchers.IkePhase1ProposalMatchers;
import org.batfish.datamodel.matchers.IpAccessListMatchers;
import org.batfish.datamodel.matchers.IpsecPeerConfigMatchers;
import org.batfish.datamodel.matchers.IpsecPhase2PolicyMatchers;
import org.batfish.datamodel.matchers.IpsecPhase2ProposalMatchers;
import org.batfish.datamodel.matchers.IsisInterfaceLevelSettingsMatchers;
import org.batfish.datamodel.matchers.IsisInterfaceSettingsMatchers;
import org.batfish.datamodel.matchers.IsisProcessMatchers;
import org.batfish.datamodel.matchers.OspfAreaMatchers;
import org.batfish.datamodel.matchers.RouteFilterListMatchers;
import org.batfish.datamodel.matchers.StubSettingsMatchers;
import org.batfish.datamodel.ospf.OspfArea;
import org.batfish.datamodel.ospf.OspfAreaSummary;
import org.batfish.datamodel.ospf.OspfDefaultOriginateType;
import org.batfish.datamodel.ospf.OspfMetricType;
import org.batfish.datamodel.ospf.OspfNetworkType;
import org.batfish.datamodel.ospf.OspfProcess;
import org.batfish.datamodel.ospf.StubType;
import org.batfish.datamodel.route.nh.NextHopDiscard;
import org.batfish.datamodel.route.nh.NextHopIp;
import org.batfish.datamodel.route.nh.NextHopVrf;
import org.batfish.datamodel.routing_policy.Environment;
import org.batfish.datamodel.routing_policy.Environment.Direction;
import org.batfish.datamodel.routing_policy.Result;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.expr.MatchProtocol;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.datamodel.routing_policy.statement.SetAdministrativeCost;
import org.batfish.datamodel.routing_policy.statement.TraceableStatement;
import org.batfish.datamodel.tracking.TrackMethods;
import org.batfish.datamodel.transformation.AssignIpAddressFromPool;
import org.batfish.datamodel.transformation.AssignPortFromPool;
import org.batfish.datamodel.transformation.IpField;
import org.batfish.datamodel.transformation.Noop;
import org.batfish.datamodel.transformation.ShiftIpAddressIntoSubnet;
import org.batfish.datamodel.transformation.Transformation;
import org.batfish.dataplane.ibdp.IncrementalDataPlane;
import org.batfish.grammar.BatfishParseTreeWalker;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Flat_juniper_configurationContext;
import org.batfish.grammar.silent_syntax.SilentSyntaxCollection;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.batfish.main.TestrigText;
import org.batfish.representation.juniper.AllVlans;
import org.batfish.representation.juniper.ApplicationSetMember;
import org.batfish.representation.juniper.BaseApplication;
import org.batfish.representation.juniper.BgpGroup;
import org.batfish.representation.juniper.BridgeDomain;
import org.batfish.representation.juniper.BridgeDomainVlanIdAll;
import org.batfish.representation.juniper.BridgeDomainVlanIdNone;
import org.batfish.representation.juniper.BridgeDomainVlanIdNumber;
import org.batfish.representation.juniper.ConcreteFirewallFilter;
import org.batfish.representation.juniper.Condition;
import org.batfish.representation.juniper.DhcpRelayServerGroup;
import org.batfish.representation.juniper.DscpUtil;
import org.batfish.representation.juniper.EvpnEncapsulation;
import org.batfish.representation.juniper.FirewallFilter;
import org.batfish.representation.juniper.FwFrom;
import org.batfish.representation.juniper.FwFromDestinationPort;
import org.batfish.representation.juniper.FwFromFragmentOffset;
import org.batfish.representation.juniper.FwFromIcmpCode;
import org.batfish.representation.juniper.FwFromIcmpCodeExcept;
import org.batfish.representation.juniper.FwFromIcmpType;
import org.batfish.representation.juniper.FwFromIcmpTypeExcept;
import org.batfish.representation.juniper.FwFromInterface;
import org.batfish.representation.juniper.FwFromInterfaceSet;
import org.batfish.representation.juniper.FwFromPacketLength;
import org.batfish.representation.juniper.FwFromPort;
import org.batfish.representation.juniper.FwFromSourcePort;
import org.batfish.representation.juniper.FwTerm;
import org.batfish.representation.juniper.FwThenAccept;
import org.batfish.representation.juniper.IcmpLarge;
import org.batfish.representation.juniper.InterfaceOspfNeighbor;
import org.batfish.representation.juniper.InterfaceRange;
import org.batfish.representation.juniper.InterfaceRangeMember;
import org.batfish.representation.juniper.InterfaceRangeMemberRange;
import org.batfish.representation.juniper.IpBgpGroup;
import org.batfish.representation.juniper.IpUnknownProtocol;
import org.batfish.representation.juniper.JuniperConfiguration;
import org.batfish.representation.juniper.JuniperStructureType;
import org.batfish.representation.juniper.JuniperStructureUsage;
import org.batfish.representation.juniper.MulticastModeOptions;
import org.batfish.representation.juniper.NamedBgpGroup;
import org.batfish.representation.juniper.Nat;
import org.batfish.representation.juniper.Nat.Type;
import org.batfish.representation.juniper.NatPacketLocation;
import org.batfish.representation.juniper.NatPool;
import org.batfish.representation.juniper.NatRule;
import org.batfish.representation.juniper.NatRuleMatchDstAddr;
import org.batfish.representation.juniper.NatRuleMatchDstAddrName;
import org.batfish.representation.juniper.NatRuleMatchDstPort;
import org.batfish.representation.juniper.NatRuleMatchProtocol;
import org.batfish.representation.juniper.NatRuleMatchSrcAddr;
import org.batfish.representation.juniper.NatRuleMatchSrcAddrName;
import org.batfish.representation.juniper.NatRuleMatchSrcPort;
import org.batfish.representation.juniper.NatRuleSet;
import org.batfish.representation.juniper.NatRuleThenInterface;
import org.batfish.representation.juniper.NatRuleThenOff;
import org.batfish.representation.juniper.NatRuleThenPool;
import org.batfish.representation.juniper.NatRuleThenPrefix;
import org.batfish.representation.juniper.NatRuleThenPrefixName;
import org.batfish.representation.juniper.NextHop;
import org.batfish.representation.juniper.NoPortTranslation;
import org.batfish.representation.juniper.OspfInterfaceSettings;
import org.batfish.representation.juniper.PatPool;
import org.batfish.representation.juniper.PathSelectionMode;
import org.batfish.representation.juniper.PolicyStatement;
import org.batfish.representation.juniper.PsFromColor;
import org.batfish.representation.juniper.PsFromCondition;
import org.batfish.representation.juniper.PsFromExternal;
import org.batfish.representation.juniper.PsFromLocalPreference;
import org.batfish.representation.juniper.PsFromTag;
import org.batfish.representation.juniper.PsTerm;
import org.batfish.representation.juniper.PsThenAsPathExpandAsList;
import org.batfish.representation.juniper.PsThenAsPathExpandLastAs;
import org.batfish.representation.juniper.PsThenAsPathPrepend;
import org.batfish.representation.juniper.PsThenCommunityAdd;
import org.batfish.representation.juniper.PsThenCommunitySet;
import org.batfish.representation.juniper.PsThenLocalPreference;
import org.batfish.representation.juniper.PsThenLocalPreference.Operator;
import org.batfish.representation.juniper.PsThenTag;
import org.batfish.representation.juniper.PsThenTunnelAttributeRemove;
import org.batfish.representation.juniper.PsThenTunnelAttributeSet;
import org.batfish.representation.juniper.Resolution;
import org.batfish.representation.juniper.ResolutionRib;
import org.batfish.representation.juniper.RoutingInformationBase;
import org.batfish.representation.juniper.RoutingInstance;
import org.batfish.representation.juniper.Screen;
import org.batfish.representation.juniper.ScreenAction;
import org.batfish.representation.juniper.ScreenOption;
import org.batfish.representation.juniper.Srlg;
import org.batfish.representation.juniper.StaticRouteV4;
import org.batfish.representation.juniper.StaticRouteV6;
import org.batfish.representation.juniper.TcpFinNoAck;
import org.batfish.representation.juniper.TcpNoFlag;
import org.batfish.representation.juniper.TcpSynFin;
import org.batfish.representation.juniper.TunnelAttribute;
import org.batfish.representation.juniper.VlanRange;
import org.batfish.representation.juniper.VlanReference;
import org.batfish.representation.juniper.VrrpGroup;
import org.batfish.representation.juniper.Zone;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

/** Tests for {@link FlatJuniperParser} and {@link FlatJuniperControlPlaneExtractor}. */
public final class FlatJuniperGrammarTest {

  private static final String TESTCONFIGS_PREFIX = "org/batfish/grammar/juniper/testconfigs/";

  private static final String TESTRIGS_PREFIX = "org/batfish/grammar/juniper/testrigs/";

  @Rule public TemporaryFolder _folder = new TemporaryFolder();

  @Rule public ExpectedException _thrown = ExpectedException.none();

  private static Flow createFlow(String sourceAddress, String destinationAddress) {
    return createFlow(Ip.parse(sourceAddress), Ip.parse(destinationAddress));
  }

  private static Flow createFlow(Ip src, Ip dst) {
    return Flow.builder().setIngressNode("node").setSrcIp(src).setDstIp(dst).build();
  }

  private static Flow createFlow(IpProtocol protocol, int port) {
    Flow.Builder fb =
        builder().setIngressNode("node").setIpProtocol(protocol).setDstPort(port).setSrcPort(port);
    return fb.build();
  }

  private boolean routingPolicyPermitsRoute(RoutingPolicy routingPolicy, AbstractRoute route) {
    return routingPolicy.process(
        route,
        route instanceof Bgpv4Route
            ? route.toBuilder()
            : Bgpv4Route.testBuilder().setNetwork(route.getNetwork()),
        Direction.OUT);
  }

  private Batfish getBatfishForConfigurationNames(String... configurationNames) throws IOException {
    String[] names =
        Arrays.stream(configurationNames).map(s -> TESTCONFIGS_PREFIX + s).toArray(String[]::new);
    return BatfishTestUtils.getBatfishForTextConfigs(_folder, names);
  }

  private Configuration parseConfig(String hostname) {
    try {
      Map<String, Configuration> configs = parseTextConfigs(hostname);
      assertThat(configs, hasKey(hostname.toLowerCase()));
      return configs.get(hostname.toLowerCase());
    } catch (IOException e) {
      throw new AssertionError("Failed to parse " + hostname, e);
    }
  }

  private JuniperConfiguration parseJuniperConfig(String hostname) {
    String src = readResource(TESTCONFIGS_PREFIX + hostname, UTF_8);
    Settings settings = new Settings();
    BatfishTestUtils.configureBatfishTestSettings(settings);
    FlatJuniperCombinedParser flatJuniperParser =
        new FlatJuniperCombinedParser(src, settings, null);
    Warnings w = new Warnings();
    FlatJuniperControlPlaneExtractor extractor =
        new FlatJuniperControlPlaneExtractor(
            src, flatJuniperParser, w, new SilentSyntaxCollection());
    ParserRuleContext tree =
        Batfish.parse(
            flatJuniperParser, new BatfishLogger(BatfishLogger.LEVELSTR_FATAL, false), settings);
    extractor.processParseTree(DUMMY_SNAPSHOT_1, tree);
    JuniperConfiguration ret =
        SerializationUtils.clone((JuniperConfiguration) extractor.getVendorConfiguration());
    ret.setWarnings(w);
    return ret;
  }

  private Map<String, Configuration> parseTextConfigs(String... configurationNames)
      throws IOException {
    IBatfish iBatfish = getBatfishForConfigurationNames(configurationNames);
    return iBatfish.loadConfigurations(iBatfish.getSnapshot());
  }

  @Test
  public void testApplicationsExtraction() {
    String hostname = "applications";
    JuniperConfiguration vc = parseJuniperConfig(hostname);
    assertThat(
        vc.getMasterLogicalSystem().getApplications(), hasKeys("a1", "a2", "a3", "a4", "a5"));
    // TODO: a1-a3
    {
      BaseApplication application = vc.getMasterLogicalSystem().getApplications().get("a4");
      assertThat(
          _b.toBDD(application.getMainTerm().getHeaderSpace()),
          equalTo(_b.toBDD(and(matchIpProtocol(ICMP), matchIcmpType(5)))));
    }
    {
      BaseApplication application = vc.getMasterLogicalSystem().getApplications().get("a5");
      assertThat(
          _b.toBDD(application.getMainTerm().getHeaderSpace()),
          equalTo(_b.toBDD(and(matchIpProtocol(ICMP), matchIcmpCode(6)))));
    }
  }

  @Test
  public void testApplicationsReferences() throws IOException {
    String hostname = "applications";
    String filename = "configs/" + hostname;

    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    /* Confirm application usage is tracked properly */
    assertThat(ccae, hasNumReferrers(filename, APPLICATION, "a2", 0));
    assertThat(ccae, hasNumReferrers(filename, APPLICATION, "a1", 1));
    assertThat(ccae, hasNumReferrers(filename, APPLICATION, "a3", 1));

    /* Confirm undefined reference is identified */
    assertThat(ccae, hasUndefinedReference(filename, APPLICATION_OR_APPLICATION_SET, "a_undef"));
  }

  @Test
  public void testApplicationSet() throws IOException {
    String hostname = "application-set";
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());
    Configuration c = parseConfig(hostname);

    /* Check that appset2 contains definition of appset1 concatenated with definition of a3 */
    assertThat(
        c,
        hasIpAccessList(
            ACL_NAME_GLOBAL_POLICY,
            IpAccessListMatchers.hasLines(
                equalTo(
                    ImmutableList.of(
                        new ExprAclLine(
                            LineAction.PERMIT,
                            new OrMatchExpr(
                                ImmutableList.of(
                                    // appset1
                                    new OrMatchExpr(
                                        ImmutableList.of(
                                            // a1
                                            new MatchHeaderSpace(
                                                HeaderSpace.builder()
                                                    .setIpProtocols(
                                                        ImmutableList.of(IpProtocol.TCP))
                                                    .setSrcPorts(
                                                        ImmutableList.of(SubRange.singleton(1)))
                                                    .build(),
                                                ApplicationSetMember
                                                    .getTraceElementForUserApplication(
                                                        filename, APPLICATION, "a1")),
                                            // a2
                                            new MatchHeaderSpace(
                                                HeaderSpace.builder()
                                                    .setIpProtocols(
                                                        ImmutableList.of(IpProtocol.UDP))
                                                    .setDstPorts(
                                                        ImmutableList.of(SubRange.singleton(2)))
                                                    .build(),
                                                ApplicationSetMember
                                                    .getTraceElementForUserApplication(
                                                        filename, APPLICATION, "a2"))),
                                        ApplicationSetMember.getTraceElementForUserApplication(
                                            filename, APPLICATION_SET, "appset1")),
                                    // a3
                                    new MatchHeaderSpace(
                                        HeaderSpace.builder()
                                            .setIpProtocols(ImmutableList.of(IpProtocol.UDP))
                                            .setDstPorts(ImmutableList.of(SubRange.singleton(3)))
                                            .build(),
                                        ApplicationSetMember.getTraceElementForUserApplication(
                                            filename, APPLICATION, "a3"))),
                                ApplicationSetMember.getTraceElementForUserApplication(
                                    filename, APPLICATION_SET, "appset2")),
                            "p1",
                            matchingSecurityPolicyTerm(filename, ACL_NAME_GLOBAL_POLICY, "p1"),
                            securityPolicyTermVendorStructureId(
                                filename, ACL_NAME_GLOBAL_POLICY, "p1")))))));

    /* Check that appset1 and appset2 are referenced, but appset3 is not */
    assertThat(ccae, hasNumReferrers(filename, APPLICATION_SET, "appset1", 1));
    assertThat(ccae, hasNumReferrers(filename, APPLICATION_SET, "appset2", 1));
    assertThat(ccae, hasNumReferrers(filename, APPLICATION_SET, "appset3", 0));

    /*
     * Check that there is an undefined reference to appset4, but not to appset1-3
     * (via reference in security policy).
     */
    assertThat(
        ccae,
        hasUndefinedReference(
            filename,
            APPLICATION_OR_APPLICATION_SET,
            "appset4",
            SECURITY_POLICY_MATCH_APPLICATION));
    assertThat(
        ccae, not(hasUndefinedReference(hostname, APPLICATION_OR_APPLICATION_SET, "appset1")));
    assertThat(
        ccae, not(hasUndefinedReference(hostname, APPLICATION_OR_APPLICATION_SET, "appset2")));
    assertThat(
        ccae, not(hasUndefinedReference(hostname, APPLICATION_OR_APPLICATION_SET, "appset3")));

    /*
     * Check that there is an undefined reference to application-set appset4, but not to appset1-3
     * (via reference in application-set definition).
     */
    assertThat(
        ccae,
        hasUndefinedReference(
            filename, APPLICATION_SET, "appset4", APPLICATION_SET_MEMBER_APPLICATION_SET));
    assertThat(ccae, not(hasUndefinedReference(filename, APPLICATION_SET, "appset1")));
    assertThat(ccae, not(hasUndefinedReference(filename, APPLICATION_SET, "appset2")));
    assertThat(ccae, not(hasUndefinedReference(filename, APPLICATION_SET, "appset3")));

    /*
     * Check that there is an undefined reference to application a4 but not a1-3
     * (via reference in application-set definition).
     */
    assertThat(
        ccae,
        hasUndefinedReference(
            filename, APPLICATION_OR_APPLICATION_SET, "a4", APPLICATION_SET_MEMBER_APPLICATION));
    assertThat(ccae, not(hasUndefinedReference(filename, APPLICATION_OR_APPLICATION_SET, "a1")));
    assertThat(ccae, not(hasUndefinedReference(filename, APPLICATION_OR_APPLICATION_SET, "a2")));
    assertThat(ccae, not(hasUndefinedReference(filename, APPLICATION_OR_APPLICATION_SET, "a3")));
  }

  @Test
  public void testApplicationSetNested() {
    String hostname = "application-set-nested";
    Configuration c = parseConfig(hostname);

    String aclNameNonNested = zoneToZoneFilter("z1", "z2");
    String aclNameNested = zoneToZoneFilter("z1", "z3");
    String aclNameMultiNested = zoneToZoneFilter("z1", "z4");
    String z1Interface = "ge-0/0/0.0";
    IpAccessList aclNonNested = c.getIpAccessLists().get(aclNameNonNested);
    IpAccessList aclNested = c.getIpAccessLists().get(aclNameNested);
    IpAccessList aclMultiNested = c.getIpAccessLists().get(aclNameMultiNested);
    /* Allowed application permits TCP from port 1 only */
    Flow permittedFlow = createFlow(IpProtocol.TCP, 1);
    Flow rejectedFlow = createFlow(IpProtocol.TCP, 2);

    /*
     * Confirm non-nested application-set acl accepts the allowed protocol-port combo and reject
     * others
     */
    assertThat(
        aclNonNested, accepts(permittedFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        aclNonNested, rejects(rejectedFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));

    /*
     * Confirm nested application-set acl accepts the allowed protocol-port combo and reject others
     */
    assertThat(
        aclNested, accepts(permittedFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        aclNested, rejects(rejectedFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));

    /*
     * Confirm multi-nested application-set acl accepts the allowed protocol-port combo and reject
     * others
     */
    assertThat(
        aclMultiNested, accepts(permittedFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        aclMultiNested, rejects(rejectedFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));
  }

  @Test
  public void testApplicationWithTerms() {
    String hostname = "application-with-terms";
    Configuration c = parseConfig(hostname);

    // An IpAccessList should be generated for the cross-zone policy from z1 to z2. Each policy
    // should correspond to one line.
    assertThat(
        c,
        hasIpAccessList(
            ACL_NAME_GLOBAL_POLICY,
            IpAccessListMatchers.hasLines(
                equalTo(
                    ImmutableList.of(
                        new ExprAclLine(
                            LineAction.PERMIT,
                            new OrMatchExpr(
                                ImmutableList.of(
                                    // term 1
                                    new MatchHeaderSpace(
                                        HeaderSpace.builder()
                                            .setDstPorts(ImmutableList.of(SubRange.singleton(1)))
                                            .setIpProtocols(ImmutableList.of(IpProtocol.TCP))
                                            .setSrcPorts(ImmutableList.of(SubRange.singleton(2)))
                                            .build(),
                                        TraceElement.of("Matched term t1")),
                                    // term 2
                                    new MatchHeaderSpace(
                                        HeaderSpace.builder()
                                            .setDstPorts(ImmutableList.of(SubRange.singleton(3)))
                                            .setIpProtocols(ImmutableList.of(IpProtocol.UDP))
                                            .setSrcPorts(ImmutableList.of(SubRange.singleton(4)))
                                            .build(),
                                        TraceElement.of("Matched term t2"))),
                                ApplicationSetMember.getTraceElementForUserApplication(
                                    "configs/" + c.getHostname(), APPLICATION, "a1")),
                            "p1",
                            matchingSecurityPolicyTerm(
                                "configs/" + c.getHostname(), ACL_NAME_GLOBAL_POLICY, "p1"),
                            securityPolicyTermVendorStructureId(
                                "configs/" + c.getHostname(), ACL_NAME_GLOBAL_POLICY, "p1")))))));
  }

  @Test
  public void testAuthenticationKeyChain() throws IOException {
    String hostname = "authentication-key-chain";
    String filename = "configs/" + hostname;

    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    /* Confirm filter usage is tracked properly */
    assertThat(ccae, hasNumReferrers(filename, AUTHENTICATION_KEY_CHAIN, "KC", 1));
    assertThat(ccae, hasNumReferrers(filename, AUTHENTICATION_KEY_CHAIN, "KC_UNUSED", 0));

    /* Confirm undefined reference is identified */
    assertThat(ccae, hasUndefinedReference(filename, AUTHENTICATION_KEY_CHAIN, "KC_UNDEF"));
  }

  @Test
  public void testAuthenticationOrder() {
    String hostname = "authentication-order";

    Configuration configuration = parseConfig(hostname);
    SortedMap<String, Line> lines = configuration.getVendorFamily().getJuniper().getLines();

    assertThat(lines.get(AUXILIARY_LINE_NAME), nullValue());

    assertThat(
        lines.get(CONSOLE_LINE_NAME),
        hasAuthenticationLoginList(hasMethods(equalTo(Collections.singletonList(GROUP_TACACS)))));

    assertThat(
        lines.get("telnet"),
        hasAuthenticationLoginList(hasMethods(equalTo(Arrays.asList(GROUP_TACACS, PASSWORD)))));

    assertThat(
        lines.get("ssh"),
        hasAuthenticationLoginList(hasMethods(equalTo(Arrays.asList(GROUP_RADIUS, GROUP_TACACS)))));

    assertThat(
        lines.get("ftp"),
        hasAuthenticationLoginList(hasMethods(equalTo(Collections.singletonList(GROUP_RADIUS)))));
  }

  @Test
  public void testAutonomousSystem() throws IOException {
    String testrigName = "autonomous-system";
    String c1Name = "as1";
    String c2Name = "as2";
    String c3Name = "as3";
    Ip neighborIp = Ip.parse("1.0.0.1");

    List<String> configurationNames = ImmutableList.of(c1Name, c2Name, c3Name);
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationFiles(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations(batfish.getSnapshot());
    Configuration c1 = configurations.get(c1Name);
    Configuration c2 = configurations.get(c2Name);
    Configuration c3 = configurations.get(c3Name);

    assertThat(c1, hasDefaultVrf(hasBgpProcess(hasActiveNeighbor(neighborIp, hasLocalAs(1L)))));
    assertThat(c2, hasDefaultVrf(hasBgpProcess(hasActiveNeighbor(neighborIp, hasLocalAs(1L)))));
    assertThat(c3, hasDefaultVrf(hasBgpProcess(hasActiveNeighbor(neighborIp, hasLocalAs(1L)))));
  }

  @Test
  public void testStaticRouteCommunities() throws IOException {
    /*
    Setup: r1 has a BGP import policy that rejects routes with community 100:1001.
    r2 exports:
    - static route 10.20.20.0/24 with community 100:1001
    - static route 10.20.20.0/23 with community 100:1002
    - static route 10.20.22.0/24, no communities
    r1 should reject first route, but install the others in its RIB.
     */
    String testrigName = "static-route-communities";
    String c1Name = "r1";
    String c2Name = "r2";
    StandardCommunity acceptedCommunity = StandardCommunity.parse("100:1002");

    List<String> configurationNames = ImmutableList.of(c1Name, c2Name);
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationFiles(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    batfish.computeDataPlane(batfish.getSnapshot());
    DataPlane dp = batfish.loadDataPlane(batfish.getSnapshot());
    Set<AbstractRoute> r1Routes = dp.getRibs().get(c1Name, DEFAULT_VRF_NAME).getRoutes();

    assertThat(r1Routes, not(hasItem(hasPrefix(Prefix.parse("10.20.20.0/24")))));
    assertThat(
        r1Routes,
        hasItem(
            allOf(hasPrefix(Prefix.parse("10.20.20.0/23")), hasCommunities(acceptedCommunity))));
    assertThat(
        r1Routes, hasItem(allOf(hasPrefix(Prefix.parse("10.20.22.0/24")), hasCommunities())));
  }

  @Test
  public void testAutonomousSystemLoops() {
    Configuration c = parseConfig("autonomous-system-loops");
    assertThat(
        c,
        hasDefaultVrf(
            hasBgpProcess(
                hasActiveNeighbor(
                    Ip.parse("2.2.2.2"),
                    allOf(
                        hasIpv4UnicastAddressFamily(
                            hasAddressFamilyCapabilites(hasAllowLocalAsIn(true))),
                        hasLocalAs(1L))))));
    assertThat(
        c,
        hasVrf(
            "FOO",
            hasBgpProcess(
                hasActiveNeighbor(
                    Ip.parse("3.3.3.3"),
                    allOf(
                        hasIpv4UnicastAddressFamily(
                            hasAddressFamilyCapabilites(hasAllowLocalAsIn(true))),
                        hasLocalAs(1L))))));
  }

  @Test
  public void testAutonomousSystemLoopsNonDefaultRoutingInstance() {
    Configuration c = parseConfig("autonomous-system-loops-routing-instance");
    assertThat(
        c,
        hasVrf(
            "FOO",
            hasBgpProcess(
                hasActiveNeighbor(
                    Ip.parse("2.2.2.2"),
                    allOf(
                        hasIpv4UnicastAddressFamily(
                            hasAddressFamilyCapabilites(hasAllowLocalAsIn(true))),
                        hasLocalAs(1L))))));
  }

  /** Tests support for dynamic bgp parsing using "bgp allow" command */
  @Test
  public void testBgpAllow() {
    Configuration c = parseConfig("bgp-allow");
    assertThat(
        c,
        hasDefaultVrf(hasBgpProcess(hasPassiveNeighbor(Prefix.parse("10.1.1.0/24"), anything()))));
  }

  @Test
  public void testBgpBmpParsing() {
    parseJuniperConfig("juniper-bgp-bmp");
  }

  @Test
  public void testClassOfServiceParsing() {
    parseJuniperConfig("juniper-class-of-service");
  }

  @Test
  public void testL2Topology() throws IOException {
    /*
    L1:
      - r1, r2, and fw are all connected to the central switch sw as shown below
      - fw has self-adjacencies xe-0/0/10 <-> xe-0/0/20 and xe-0/0/11 <-> xe-0/0/21
    L2:
      - sw[xe-0/0/0] is in trunk mode with allowed VLANs 10 and 11
      - sw[xe-0/0/1] is in trunk mode with allowed VLANs 20 and 21
      - r1[xe-0/0/0] has subunits 10 and 11 using 802.1q encapsulation
      - r2[xe-0/0/1] has subunits 20 and 21 using 802.1q encapsulation
      - sw[xe-0/0/3] and fw[xe-0/0/3] are both in trunk mode with allowed VLANs 10, 11, 20, and 21
      - fw's other interfaces are in access mode with VLAN ID corresponding to iface number;
        so its L1 self-adjacencies connect VLAN 10 to 20 and VLAN 11 to 21
      - This should create two broadcast domains, one for VLANS 10 and 20 and one for VLANS 11 and 21.
    L3:
      - Only r1 and r2's interfaces have IP addresses. Thanks to L2 setup, should see L3 edges:
        - r1[xe-0/0/0.10] <-> r2[xe-0/0/1.20]
        - r1[xe-0/0/0.11] <-> r2[xe-0/0/1.21]

               fw
            [xe-0/0/3]
                |
                |
            [xe-0/0/3]
               sw
      [xe-0/0/0] [xe-0/0/1]
          /             \
         /               \
    [xe-0/0/0]         [xe-0/0/1]
       r1                  r2
    */
    String snapshotName = "l2-topology";
    String fw = "fw";
    String r1 = "r1";
    String r2 = "r2";
    String sw = "sw";
    String resourcePrefix = TESTRIGS_PREFIX + snapshotName;
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setLayer1TopologyPrefix(resourcePrefix)
                .setConfigurationFiles(resourcePrefix, fw, r1, r2, sw)
                .build(),
            _folder);
    batfish.loadConfigurations(batfish.getSnapshot());

    // check layer-2 adjacencies for L3 interfaces
    L3Adjacencies adjacencies =
        batfish.getTopologyProvider().getInitialL3Adjacencies(batfish.getSnapshot());
    assertTrue(
        adjacencies.inSameBroadcastDomain(
            NodeInterfacePair.of(r1, "xe-0/0/0.10"), NodeInterfacePair.of(r2, "xe-0/0/1.20")));
    assertFalse(
        adjacencies.inSameBroadcastDomain(
            NodeInterfacePair.of(r1, "xe-0/0/0.10"), NodeInterfacePair.of(r2, "xe-0/0/1.21")));
    assertTrue(
        adjacencies.inSameBroadcastDomain(
            NodeInterfacePair.of(r1, "xe-0/0/0.11"), NodeInterfacePair.of(r2, "xe-0/0/1.21")));
    assertFalse(
        adjacencies.inSameBroadcastDomain(
            NodeInterfacePair.of(r1, "xe-0/0/0.11"), NodeInterfacePair.of(r2, "xe-0/0/1.20")));

    // check layer-3 adjacencies
    Topology layer3Topology =
        batfish.getTopologyProvider().getInitialLayer3Topology(batfish.getSnapshot());
    Edge edge1 = Edge.of(r1, "xe-0/0/0.10", r2, "xe-0/0/1.20");
    Edge edge2 = Edge.of(r1, "xe-0/0/0.11", r2, "xe-0/0/1.21");
    assertThat(
        layer3Topology.getEdges(),
        containsInAnyOrder(edge1, edge1.reverse(), edge2, edge2.reverse()));
  }

  @Test
  public void testParentChildTopology() throws IOException {
    String resourcePrefix = "org/batfish/grammar/juniper/testrigs/topology";
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setLayer1TopologyPrefix(resourcePrefix)
                .setConfigurationFiles(resourcePrefix, "r1", "r2")
                .build(),
            _folder);
    batfish.loadConfigurations(batfish.getSnapshot());

    Layer1Topology layer1LogicalTopology =
        batfish.getTopologyProvider().getLayer1LogicalTopology(batfish.getSnapshot()).get();
    L3Adjacencies adjacencies =
        batfish.getTopologyProvider().getInitialL3Adjacencies(batfish.getSnapshot());
    Topology layer3Topology =
        batfish.getTopologyProvider().getInitialLayer3Topology(batfish.getSnapshot());

    // check layer-1 logical adjacencies
    assertThat(
        layer1LogicalTopology.edgeStream().collect(Collectors.toList()),
        hasItem(new Layer1Edge("r1", "ae0", "r2", "ae0")));

    // check layer-2 adjacencies
    assertThat(
        adjacencies.inSameBroadcastDomain(
            NodeInterfacePair.of("r1", "ge-0/0/0.0"), NodeInterfacePair.of("r2", "ge-0/0/0.0")),
        equalTo(true));
    assertThat(
        adjacencies.inSameBroadcastDomain(
            NodeInterfacePair.of("r1", "ge-0/0/1.0"), NodeInterfacePair.of("r2", "ge-0/0/1.0")),
        equalTo(true));
    assertThat(
        adjacencies.inSameBroadcastDomain(
            NodeInterfacePair.of("r1", "ae0.0"), NodeInterfacePair.of("r2", "ae0.0")),
        equalTo(true));

    // check layer-3 adjacencies
    assertThat(
        layer3Topology.getEdges(), not(hasItem(Edge.of("r1", "ge-0/0/0.0", "r2", "ge-0/0/0.0"))));
    assertThat(layer3Topology.getEdges(), hasItem(Edge.of("r1", "ge-0/0/1.0", "r2", "ge-0/0/1.0")));
    assertThat(layer3Topology.getEdges(), hasItem(Edge.of("r1", "ae0.0", "r2", "ae0.0")));
  }

  @Test
  public void testBgpClusterId() throws IOException {
    String testrigName = "rr";
    String configName = "rr";
    Ip neighbor1Ip = Ip.parse("2.2.2.2");
    Ip neighbor2Ip = Ip.parse("4.4.4.4");

    List<String> configurationNames = ImmutableList.of(configName);
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationFiles(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations(batfish.getSnapshot());

    Configuration rr = configurations.get(configName);
    BgpProcess proc = rr.getDefaultVrf().getBgpProcess();
    BgpPeerConfig neighbor1 = proc.getActiveNeighbors().get(neighbor1Ip);
    BgpPeerConfig neighbor2 = proc.getActiveNeighbors().get(neighbor2Ip);

    assertThat(neighbor1, hasClusterId(Ip.parse("3.3.3.3").asLong()));
    assertThat(neighbor2, hasClusterId(Ip.parse("1.1.1.1").asLong()));
  }

  @Test
  public void testMisbraced() throws IOException {
    // don't crash, do produce a config
    Batfish batfish = getBatfishForConfigurationNames("misbraced");
    batfish.getSettings().setDisableUnrecognized(false);
    assertThat(batfish.loadConfigurations(batfish.getSnapshot()), aMapWithSize(1));
  }

  @Test
  public void testBgpConfederation() {
    JuniperConfiguration c = parseJuniperConfig("bgp-confederation");
    RoutingInstance ri = c.getMasterLogicalSystem().getDefaultRoutingInstance();
    assertThat(ri.getConfederation(), equalTo(7L));
    assertThat(ri.getConfederationMembers(), contains(65001L, 65002L, 65003L));
  }

  @Test
  public void testBgpConfederationConversion() {
    Configuration c = parseConfig("bgp-confederation");
    BgpProcess bgpProcess = c.getDefaultVrf().getBgpProcess();
    assertThat(
        bgpProcess.getConfederation(),
        equalTo(new BgpConfederation(7L, ImmutableSet.of(65001L, 65002L, 65003L))));
    assertThat(
        bgpProcess.getActiveNeighbors().get(Ip.parse("1.1.1.1")).getConfederationAsn(),
        equalTo(7L));
  }

  @Test
  public void testBgpDescription() {
    Configuration c = parseConfig("bgp-description");
    Map<Ip, BgpActivePeerConfig> neighbors = c.getDefaultVrf().getBgpProcess().getActiveNeighbors();
    assertThat(neighbors, hasKeys(Ip.parse("1.2.3.4"), Ip.parse("2.3.4.5")));
    assertThat(neighbors.get(Ip.parse("1.2.3.4")).getDescription(), equalTo("N"));
    assertThat(neighbors.get(Ip.parse("2.3.4.5")).getDescription(), nullValue());
  }

  @Test
  public void testBgpKeepExtraction() {
    JuniperConfiguration c = parseJuniperConfig("bgp-keep");

    // Routing instance R1 does not set keep
    RoutingInstance r1 = c.getMasterLogicalSystem().getRoutingInstances().get("R1");
    assertNull(r1.getMasterBgpGroup().getKeep());
    assertEquals(r1.getNamedBgpGroups().get("G1").getKeep(), BgpGroup.BgpKeepType.NONE);
    assertEquals(r1.getNamedBgpGroups().get("G2").getKeep(), BgpGroup.BgpKeepType.ALL);
    assertNull(r1.getNamedBgpGroups().get("G3").getKeep());

    // group --> neighbor inheritance
    IpBgpGroup n1 = r1.getIpBgpGroups().get(Prefix.parse("1.1.1.1/32"));
    n1.cascadeInheritance();
    assertEquals(n1.getKeep(), BgpGroup.BgpKeepType.ALL); // override

    IpBgpGroup n2 = r1.getIpBgpGroups().get(Prefix.parse("2.2.2.2/32"));
    n2.cascadeInheritance();
    assertEquals(n2.getKeep(), BgpGroup.BgpKeepType.NONE); // inherit from group

    // Routing instance R2 does set keep
    RoutingInstance r2 = c.getMasterLogicalSystem().getRoutingInstances().get("R2");
    assertEquals(r2.getMasterBgpGroup().getKeep(), BgpGroup.BgpKeepType.NONE);
    assertEquals(r2.getNamedBgpGroups().get("G1").getKeep(), BgpGroup.BgpKeepType.NONE);
    assertEquals(r2.getNamedBgpGroups().get("G2").getKeep(), BgpGroup.BgpKeepType.ALL);

    NamedBgpGroup g3 = r2.getNamedBgpGroups().get("G3");
    g3.cascadeInheritance();
    assertEquals(g3.getKeep(), BgpGroup.BgpKeepType.NONE);

    // routing-instance --> neighbor inheritance
    n1 = r2.getIpBgpGroups().get(Prefix.parse("1.1.1.1/32"));
    n1.cascadeInheritance();
    assertEquals(n1.getKeep(), BgpGroup.BgpKeepType.ALL); // override

    n2 = r2.getIpBgpGroups().get(Prefix.parse("2.2.2.2/32"));
    n2.cascadeInheritance();
    assertEquals(n2.getKeep(), BgpGroup.BgpKeepType.NONE); // inherit from routing instance
  }

  @Test
  public void testBgpMultipath() {
    assertThat(
        parseConfig("bgp-multipath").getDefaultVrf(),
        hasBgpProcess(allOf(hasMultipathEbgp(true), hasMultipathIbgp(false))));

    assertThat(
        parseConfig("bgp-multipath-internal").getDefaultVrf(),
        hasBgpProcess(allOf(hasMultipathEbgp(false), hasMultipathIbgp(true))));

    assertThat(
        parseConfig("bgp-multipath-external").getDefaultVrf(),
        hasBgpProcess(allOf(hasMultipathEbgp(true), hasMultipathIbgp(false))));
    assertThat(
        parseConfig("bgp-multipath-none").getDefaultVrf(),
        hasBgpProcess(allOf(hasMultipathEbgp(false), hasMultipathIbgp(false))));
  }

  @Test
  public void testBgpNeighborExtraction() throws IOException {
    String hostname = "bgp-neighbor";
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    String bgpNeighborStructureName = Names.bgpNeighborStructureName("1.2.3.4", "default");
    String bgpNeighborStructureName6 =
        Names.bgpNeighborStructureName("2001:db8:85a3::8a2e:370:7334", "default");
    assertThat(
        ccae,
        hasDefinedStructureWithDefinitionLines(filename, BGP_GROUP, "G", containsInAnyOrder(4, 5)));
    assertThat(
        ccae,
        hasDefinedStructureWithDefinitionLines(
            filename, BGP_NEIGHBOR, bgpNeighborStructureName, contains(4)));
    assertThat(
        ccae,
        hasDefinedStructureWithDefinitionLines(
            filename, BGP_NEIGHBOR, bgpNeighborStructureName6, contains(5)));

    assertThat(ccae, hasNumReferrers(filename, BGP_GROUP, "G", 2));
    assertThat(ccae, hasNumReferrers(filename, BGP_NEIGHBOR, bgpNeighborStructureName, 1));
    assertThat(ccae, hasNumReferrers(filename, BGP_NEIGHBOR, bgpNeighborStructureName6, 1));
  }

  @Test
  public void testBgpPriority() {
    // Just don't crash.
    parseJuniperConfig("bgp-output-queue-priority");
  }

  @Test
  public void testBgpPreferenceExtraction() {
    JuniperConfiguration c = parseJuniperConfig("bgp-preference");
    RoutingInstance ri = c.getMasterLogicalSystem().getDefaultRoutingInstance();
    assertThat(ri.getMasterBgpGroup().getPreference(), equalTo(140));
    assertThat(ri.getNamedBgpGroups().get("MYGROUP").getPreference(), nullValue());
    assertThat(ri.getNamedBgpGroups().get("MYGROUP").getPreference(), nullValue());
    assertThat(ri.getIpBgpGroups().get(Prefix.parse("1.1.1.1/32")).getPreference(), equalTo(150));
  }

  @Test
  public void testBgpPreferenceConversion() {
    Configuration c = parseConfig("bgp-preference");
    Vrf def = c.getDefaultVrf();
    assertThat(def.getBgpProcess().getAdminCost(RoutingProtocol.BGP), equalTo(140));
    assertThat(def.getBgpProcess().getAdminCost(RoutingProtocol.IBGP), equalTo(140));

    BgpProcess bgpProcess = def.getBgpProcess();
    assertNotNull(bgpProcess);
    BgpActivePeerConfig neighbor = bgpProcess.getActiveNeighbors().get(Ip.parse("1.1.1.1"));
    assertNotNull(neighbor);
    String inName = neighbor.getAddressFamily(AddressFamily.Type.IPV4_UNICAST).getImportPolicy();
    assertThat(c.getRoutingPolicies(), hasKey(inName));
    RoutingPolicy in = c.getRoutingPolicies().get(inName);

    Bgpv4Route.Builder received =
        Bgpv4Route.testBuilder()
            .setNetwork(Prefix.ZERO)
            .setOriginatorIp(Ip.parse("2.2.2.2"))
            .setOriginType(OriginType.INCOMPLETE)
            .setProtocol(RoutingProtocol.BGP)
            .setAdmin(140);
    in.process(received.build(), received, IN);
    assertThat(received.build(), hasAdministrativeCost(150));
  }

  /** For https://github.com/batfish/batfish/issues/6710 */
  @Test
  public void testBgpRoutingOptionsAutonomousSystemGH6710() throws IOException {
    String hostname = "bgp-routing-options-as-gh-6710";
    String filename = "configs/" + hostname;
    IBatfish batfish = getBatfishForConfigurationNames(hostname);
    Configuration c = batfish.loadConfigurations(batfish.getSnapshot()).get(hostname);
    Vrf def = c.getDefaultVrf();
    assertThat(def.getBgpProcess(), notNullValue());
    // Peer in group_a should pick up local-as
    assertThat(
        def.getBgpProcess().getActiveNeighbors(),
        hasEntry(equalTo(Ip.parse("10.255.16.23")), hasLocalAs(64611L)));
    // Peer in group_b should pick up routing-options autonomous-system
    assertThat(
        def.getBgpProcess().getActiveNeighbors(),
        hasEntry(equalTo(Ip.parse("10.255.42.23")), hasLocalAs(1111L)));

    List<ParseWarning> parseWarnings =
        batfish
            .loadParseVendorConfigurationAnswerElement(batfish.getSnapshot())
            .getWarnings()
            .get(filename)
            .getParseWarnings();
    assertThat(
        parseWarnings,
        hasItem(allOf(hasComment("This feature is not currently supported"), hasText("private"))));
    SortedMap<String, Warnings> convertWarnings =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot()).getWarnings();
    assertThat(
        convertWarnings.getOrDefault(hostname, new Warnings()).getRedFlagWarnings(),
        not(
            hasItem(
                WarningMatchers.hasText(
                    containsString("prepending both local-as and global-as")))));
  }

  @Test
  public void testBgpMultipathMultipleAs() throws IOException {
    String testrigName = "multipath-multiple-as";
    List<String> configurationNames =
        ImmutableList.of(
            "multiple_as_disabled",
            "multiple_as_enabled",
            "multiple_as_mixed",
            "multiple_as_mixed_conflict");

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationFiles(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations(batfish.getSnapshot());
    MultipathEquivalentAsPathMatchMode multipleAsDisabled =
        configurations
            .get("multiple_as_disabled")
            .getDefaultVrf()
            .getBgpProcess()
            .getMultipathEquivalentAsPathMatchMode();
    MultipathEquivalentAsPathMatchMode multipleAsEnabled =
        configurations
            .get("multiple_as_enabled")
            .getDefaultVrf()
            .getBgpProcess()
            .getMultipathEquivalentAsPathMatchMode();
    MultipathEquivalentAsPathMatchMode multipleAsMixed =
        configurations
            .get("multiple_as_mixed")
            .getDefaultVrf()
            .getBgpProcess()
            .getMultipathEquivalentAsPathMatchMode();
    MultipathEquivalentAsPathMatchMode mixedConflict =
        configurations
            .get("multiple_as_mixed_conflict")
            .getDefaultVrf()
            .getBgpProcess()
            .getMultipathEquivalentAsPathMatchMode();

    assertThat(multipleAsDisabled, equalTo(MultipathEquivalentAsPathMatchMode.FIRST_AS));
    assertThat(multipleAsEnabled, equalTo(MultipathEquivalentAsPathMatchMode.PATH_LENGTH));
    assertThat(multipleAsMixed, equalTo(MultipathEquivalentAsPathMatchMode.PATH_LENGTH));
    assertThat(mixedConflict, equalTo(MultipathEquivalentAsPathMatchMode.FIRST_AS));
  }

  /** Make sure bgp type internal properly sets remote as when non explicitly specified */
  @Test
  public void testBgpTypeInternalPeerAs() {
    String hostname = "bgp-type-internal";
    Configuration c = parseConfig(hostname);
    assertThat(
        c, hasDefaultVrf(hasBgpProcess(hasActiveNeighbor(Ip.parse("1.1.1.1"), hasRemoteAs(1L)))));
  }

  @Test
  public void testBgpLoops() {
    // Just don't crash.
    parseJuniperConfig("bgp-loops");
  }

  @Test
  public void testMpls() {
    // Just don't have parse warnings.
    parseJuniperConfig("juniper-mpls");
  }

  @Test
  public void testBgpMisc() {
    // Just don't crash.
    parseJuniperConfig("bgp-misc");
  }

  @Test
  public void testBgpValidation() {
    // Just don't crash.
    parseJuniperConfig("bgp-validation");
  }

  @Test
  public void testSecurityMisc() {
    // Just don't have parse warnings.
    parseJuniperConfig("juniper-security-misc");
  }

  @Test
  public void testVpn() {
    // Just don't have parse warnings.
    parseJuniperConfig("juniper-vpn");
  }

  @Test
  public void testBgpAddPathExtraction() {
    String hostname = "juniper-bgp-add-path";
    JuniperConfiguration vc = parseJuniperConfig(hostname);
    RoutingInstance ri = vc.getMasterLogicalSystem().getDefaultRoutingInstance();
    BgpGroup master = ri.getMasterBgpGroup();
    BgpGroup g1 = ri.getNamedBgpGroups().get("g1");
    BgpGroup n1 = ri.getIpBgpGroups().get(Prefix.strict("10.0.0.1/32"));
    BgpGroup g2 = ri.getNamedBgpGroups().get("g2");
    BgpGroup n2 = ri.getIpBgpGroups().get(Prefix.strict("10.0.0.2/32"));
    BgpGroup g3 = ri.getNamedBgpGroups().get("g3");
    BgpGroup n3 = ri.getIpBgpGroups().get(Prefix.strict("10.0.0.3/32"));

    assertTrue(master.getAddPath().getReceive());
    assertThat(master.getAddPath().getSend().getPathCount(), equalTo(2));
    assertNull(master.getAddPath().getSend().getPathSelectionMode());

    assertFalse(g1.getAddPath().getReceive());
    assertTrue(g1.getAddPath().getSend().getMultipath());
    assertThat(g1.getAddPath().getSend().getPathCount(), equalTo(64));

    assertNull(n1.getAddPath());

    assertNull(g2.getAddPath());

    assertFalse(n2.getAddPath().getReceive());
    assertThat(
        n2.getAddPath().getSend().getPathSelectionMode(), equalTo(PathSelectionMode.ALL_PATHS));

    assertNull(g3.getAddPath());

    assertFalse(n3.getAddPath().getReceive());
    assertThat(
        n3.getAddPath().getSend().getPathSelectionMode(),
        equalTo(PathSelectionMode.EQUAL_COST_PATHS));
    assertThat(n3.getAddPath().getSend().getPrefixPolicy(), equalTo("appp"));
  }

  @Test
  public void testBgpAddPathReferences() throws IOException {
    String hostname = "juniper-bgp-add-path";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());
    assertThat(
        ae,
        hasReferencedStructure(
            "configs/" + hostname,
            POLICY_STATEMENT,
            "appp",
            JuniperStructureUsage.ADD_PATH_SEND_PREFIX_POLICY));
  }

  @Test
  public void testBgpAddPathWarnings() throws IOException {
    String hostname = "juniper-bgp-add-path";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());
    assertThat(
        ccae.getWarnings().get(hostname).getRedFlagWarnings(),
        containsInAnyOrder(
            WarningMatchers.hasText(
                "add-path send disabled because add-path send path-count not configured for"
                    + " neighbor 10.0.0.2/32"),
            WarningMatchers.hasText(
                "add-path send disabled because add-path send path-count not configured for"
                    + " neighbor 10.0.0.3/32")));
  }

  @Test
  public void testBgpAddPathConversion() {
    String hostname = "juniper-bgp-add-path";
    Configuration c = parseConfig(hostname);
    Map<Ip, BgpActivePeerConfig> activeNeighbors =
        c.getDefaultVrf().getBgpProcess().getActiveNeighbors();
    {
      AddressFamilyCapabilities afc =
          activeNeighbors
              .get(Ip.parse("10.0.0.1"))
              .getIpv4UnicastAddressFamily()
              .getAddressFamilyCapabilities();
      assertFalse(afc.getAdditionalPathsReceive());
      assertTrue(afc.getAdditionalPathsSend());
      assertTrue(afc.getAdditionalPathsSelectAll());
    }
    {
      AddressFamilyCapabilities afc =
          activeNeighbors
              .get(Ip.parse("10.0.0.2"))
              .getIpv4UnicastAddressFamily()
              .getAddressFamilyCapabilities();
      assertFalse(afc.getAdditionalPathsReceive());
      assertFalse(afc.getAdditionalPathsSend());
      assertFalse(afc.getAdditionalPathsSelectAll());
    }
    {
      AddressFamilyCapabilities afc =
          activeNeighbors
              .get(Ip.parse("10.0.0.3"))
              .getIpv4UnicastAddressFamily()
              .getAddressFamilyCapabilities();
      assertFalse(afc.getAdditionalPathsReceive());
      assertFalse(afc.getAdditionalPathsSend());
      assertFalse(afc.getAdditionalPathsSelectAll());
    }
    {
      AddressFamilyCapabilities afc =
          activeNeighbors
              .get(Ip.parse("10.0.0.4"))
              .getIpv4UnicastAddressFamily()
              .getAddressFamilyCapabilities();
      assertTrue(afc.getAdditionalPathsReceive());
      assertTrue(afc.getAdditionalPathsSend());
      assertTrue(afc.getAdditionalPathsSelectAll());
    }
  }

  @Test
  public void testSetCommunity() {
    Configuration c = parseConfig("community");

    ConnectedRoute cr = new ConnectedRoute(Prefix.strict("1.0.0.0/24"), "blah");

    // p1
    RoutingPolicy p1 = c.getRoutingPolicies().get("p1");
    Bgpv4Route.Builder b1 =
        Bgpv4Route.testBuilder()
            .setNetwork(cr.getNetwork())
            .setCommunities(ImmutableSet.of(StandardCommunity.of(5L)))
            .setOriginatorIp(Ip.parse("2.2.2.2"))
            .setOriginType(OriginType.INCOMPLETE)
            .setProtocol(RoutingProtocol.BGP);
    p1.process(cr, b1, Direction.OUT);
    Bgpv4Route br1 = b1.build();

    assertThat(
        br1.getCommunities().getCommunities(),
        equalTo(
            ImmutableSet.of(
                StandardCommunity.NO_ADVERTISE,
                StandardCommunity.NO_EXPORT,
                StandardCommunity.NO_EXPORT_SUBCONFED)));

    // p2
    RoutingPolicy p2 = c.getRoutingPolicies().get("p2");
    Bgpv4Route.Builder b2 =
        Bgpv4Route.testBuilder()
            .setNetwork(cr.getNetwork())
            .setCommunities(ImmutableSet.of(StandardCommunity.of(5L)))
            .setOriginatorIp(Ip.parse("2.2.2.2"))
            .setOriginType(OriginType.INCOMPLETE)
            .setProtocol(RoutingProtocol.BGP);
    p2.process(cr, b2, Direction.OUT);
    Bgpv4Route br2 = b2.build();

    assertThat(
        br2.getCommunities().getCommunities(),
        equalTo(ImmutableSet.of(StandardCommunity.of(2L), StandardCommunity.of(3L))));

    // p3
    RoutingPolicy p3 = c.getRoutingPolicies().get("p3");
    Bgpv4Route.Builder b3 =
        Bgpv4Route.testBuilder()
            .setNetwork(cr.getNetwork())
            .setCommunities(ImmutableSet.of(StandardCommunity.of(5L)))
            .setOriginatorIp(Ip.parse("2.2.2.2"))
            .setOriginType(OriginType.INCOMPLETE)
            .setProtocol(RoutingProtocol.BGP);
    p3.process(cr, b3, Direction.OUT);
    Bgpv4Route br3 = b3.build();

    assertThat(
        br3.getCommunities().getCommunities(), equalTo(ImmutableSet.of(StandardCommunity.of(5L))));
  }

  @Test
  public void testAddCommunity() {
    Configuration c = parseConfig("community");

    ConnectedRoute cr = new ConnectedRoute(Prefix.strict("1.0.0.0/24"), "blah");

    // p4
    RoutingPolicy p4 = c.getRoutingPolicies().get("p4");
    Bgpv4Route.Builder b4 =
        Bgpv4Route.testBuilder()
            .setNetwork(cr.getNetwork())
            .setCommunities(ImmutableSet.of(StandardCommunity.of(5L)))
            .setOriginatorIp(Ip.parse("2.2.2.2"))
            .setOriginType(OriginType.INCOMPLETE)
            .setProtocol(RoutingProtocol.BGP);
    p4.process(cr, b4, Direction.OUT);
    Bgpv4Route br4 = b4.build();

    assertThat(
        br4.getCommunities().getCommunities(),
        equalTo(
            ImmutableSet.of(
                StandardCommunity.NO_ADVERTISE,
                StandardCommunity.NO_EXPORT,
                StandardCommunity.NO_EXPORT_SUBCONFED,
                StandardCommunity.of(5L))));

    // p5
    RoutingPolicy p5 = c.getRoutingPolicies().get("p5");
    Bgpv4Route.Builder b5 =
        Bgpv4Route.testBuilder()
            .setNetwork(cr.getNetwork())
            .setCommunities(ImmutableSet.of(StandardCommunity.of(5L)))
            .setOriginatorIp(Ip.parse("2.2.2.2"))
            .setOriginType(OriginType.INCOMPLETE)
            .setProtocol(RoutingProtocol.BGP);
    p5.process(cr, b5, Direction.OUT);
    Bgpv4Route br5 = b5.build();

    assertThat(
        br5.getCommunities().getCommunities(),
        equalTo(
            ImmutableSet.of(
                StandardCommunity.of(2L), StandardCommunity.of(3L), StandardCommunity.of(5L))));

    // p6
    RoutingPolicy p6 = c.getRoutingPolicies().get("p6");
    Bgpv4Route.Builder b6 =
        Bgpv4Route.testBuilder()
            .setNetwork(cr.getNetwork())
            .setCommunities(ImmutableSet.of(StandardCommunity.of(5L)))
            .setOriginatorIp(Ip.parse("2.2.2.2"))
            .setOriginType(OriginType.INCOMPLETE)
            .setProtocol(RoutingProtocol.BGP);
    p6.process(cr, b6, Direction.OUT);
    Bgpv4Route br6 = b6.build();

    assertThat(
        br6.getCommunities().getCommunities(), equalTo(ImmutableSet.of(StandardCommunity.of(5L))));
  }

  @Test
  public void testDeleteCommunity() {
    Configuration c = parseConfig("community");

    Bgpv4Route base =
        Bgpv4Route.testBuilder()
            .setNetwork(Prefix.ZERO)
            .setOriginatorIp(Ip.ZERO)
            .setOriginType(OriginType.INCOMPLETE)
            .setProtocol(RoutingProtocol.BGP)
            .build();

    {
      // p7 - delete well-known
      RoutingPolicy rp = c.getRoutingPolicies().get("p7");
      Bgpv4Route.Builder builder =
          base.toBuilder()
              .setCommunities(
                  ImmutableSet.of(StandardCommunity.of(5L), StandardCommunity.NO_ADVERTISE));
      rp.process(builder.build(), builder, Direction.OUT);
      Bgpv4Route outputRoute = builder.build();

      assertThat(
          outputRoute.getCommunities().getCommunities(),
          equalTo(ImmutableSet.of(StandardCommunity.of(5L))));
    }
    {
      // p8 - delete mixed
      RoutingPolicy rp = c.getRoutingPolicies().get("p8");
      Bgpv4Route.Builder builder =
          base.toBuilder()
              .setCommunities(
                  ImmutableSet.of(
                      StandardCommunity.of(0, 1),
                      StandardCommunity.of(0, 11),
                      StandardCommunity.of(0, 2),
                      StandardCommunity.of(1, 1),
                      StandardCommunity.of(0, 3)));
      rp.process(builder.build(), builder, Direction.OUT);
      Bgpv4Route outputRoute = builder.build();

      assertThat(
          outputRoute.getCommunities().getCommunities(),
          equalTo(ImmutableSet.of(StandardCommunity.of(0, 11))));
    }
    {
      // p9 - delete regex
      RoutingPolicy rp = c.getRoutingPolicies().get("p9");
      Bgpv4Route.Builder builder =
          base.toBuilder()
              .setCommunities(
                  ImmutableSet.of(StandardCommunity.of(0, 1), StandardCommunity.of(0, 2)));
      rp.process(builder.build(), builder, Direction.OUT);
      Bgpv4Route outputRoute = builder.build();

      assertThat(
          outputRoute.getCommunities().getCommunities(),
          equalTo(ImmutableSet.of(StandardCommunity.of(0, 2))));
    }
    {
      // p10 - delete inverted
      RoutingPolicy rp = c.getRoutingPolicies().get("p10");
      Bgpv4Route.Builder builder =
          base.toBuilder()
              .setCommunities(
                  ImmutableSet.of(StandardCommunity.of(0, 1), StandardCommunity.of(0, 12345)));
      rp.process(builder.build(), builder, Direction.OUT);
      Bgpv4Route outputRoute = builder.build();

      assertThat(
          outputRoute.getCommunities().getCommunities(),
          equalTo(ImmutableSet.of(StandardCommunity.of(0, 12345))));
    }
  }

  @Test
  public void testFtiInterfaceTypeSupport() {
    parseConfig("juniper_fti_interface_type");
    // don't crash.
  }

  @Test
  public void testJunosFtiConfiguration() {
    parseConfig("juniper_fti");
    // don't crash.
  }

  @Test
  public void testSetProtocolsIsis() throws IOException {
    // don't crash.
    String hostname = "juniper-set-protocols-isis";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ParseVendorConfigurationAnswerElement pvcae =
        batfish.loadParseVendorConfigurationAnswerElement(batfish.getSnapshot());
    assertEquals(pvcae.getWarnings().size(), 1);
    assertThat(
        pvcae,
        hasParseWarning(
            "configs/" + hostname, containsString("This feature is not currently supported")));
  }

  @Test
  public void testPsFromCommunity() {
    Configuration c = parseConfig("community");

    Bgpv4Route base =
        Bgpv4Route.testBuilder()
            .setNetwork(Prefix.ZERO)
            .setOriginatorIp(Ip.ZERO)
            .setOriginType(OriginType.INCOMPLETE)
            .setProtocol(RoutingProtocol.BGP)
            .build();

    {
      assertThat(c.getRoutingPolicies(), hasKey("match"));
      RoutingPolicy rp = c.getRoutingPolicies().get("match");
      // deny route with communities only matching one element of named community
      assertFalse(
          routingPolicyPermitsRoute(
              rp,
              base.toBuilder()
                  .setCommunities(ImmutableSet.of(StandardCommunity.of(0, 1)))
                  .build()));
      // permit route with communities matching all elements of named community
      assertTrue(
          routingPolicyPermitsRoute(
              rp,
              base.toBuilder()
                  .setCommunities(
                      ImmutableSet.of(
                          StandardCommunity.of(0, 1),
                          StandardCommunity.of(0, 2),
                          StandardCommunity.of(1, 1),
                          StandardCommunity.of(0, 3)))
                  .build()));
    }
    {
      assertThat(c.getRoutingPolicies(), hasKey("invert"));
      RoutingPolicy rp = c.getRoutingPolicies().get("invert");
      StandardCommunity bad = StandardCommunity.of(0, 12345);
      StandardCommunity other = StandardCommunity.of(0, 1);

      // permit base route with no communities
      assertTrue(routingPolicyPermitsRoute(rp, base));
      // deny route with the mentioned community
      assertFalse(
          routingPolicyPermitsRoute(
              rp, base.toBuilder().setCommunities(ImmutableSet.of(bad)).build()));
      // deny route that includes the mentioned community
      assertFalse(
          routingPolicyPermitsRoute(
              rp, base.toBuilder().setCommunities(ImmutableSet.of(bad, other)).build()));
      // permit route with some other community
      assertTrue(
          routingPolicyPermitsRoute(
              rp, base.toBuilder().setCommunities(ImmutableSet.of(other)).build()));
    }
  }

  @Test
  public void testPsFromNeighbor() {
    Configuration c = parseConfig("bgp-policy-from-neighbor");

    Bgpv4Route base =
        Bgpv4Route.testBuilder()
            .setNetwork(Prefix.ZERO)
            .setOriginatorIp(Ip.ZERO)
            .setOriginType(OriginType.INCOMPLETE)
            .setProtocol(RoutingProtocol.BGP)
            .build();

    Builder out_builder = org.batfish.datamodel.Bgpv4Route.builder();
    {
      // Allowed from a specific neighbor
      assertThat(c.getRoutingPolicies(), hasKey("POL"));
      RoutingPolicy rp = c.getRoutingPolicies().get("POL");
      BgpSessionProperties session_allowed =
          BgpSessionProperties.builder()
              .setRemoteAs(65500)
              .setLocalAs(65501)
              .setRemoteIp(Ip.parse("10.0.0.1"))
              .setLocalIp(Ip.parse("10.9.9.9"))
              .build();
      assertTrue(rp.processBgpRoute(base, out_builder, session_allowed, Direction.IN, null));
    }
    {
      // Rejected from wrong neighbor
      assertThat(c.getRoutingPolicies(), hasKey("POL"));
      RoutingPolicy rp = c.getRoutingPolicies().get("POL");
      BgpSessionProperties session_allowed =
          BgpSessionProperties.builder()
              .setRemoteAs(65500)
              .setLocalAs(65501)
              .setRemoteIp(Ip.parse("10.0.1.1"))
              .setLocalIp(Ip.parse("10.9.9.9"))
              .build();
      assertFalse(rp.processBgpRoute(base, out_builder, session_allowed, Direction.IN, null));
    }
  }

  @Test
  public void testPsFromNextHop() throws IOException {
    Configuration c = parseConfig("bgp-policy-from-next-hop");

    Bgpv4Route permittedRoute =
        Bgpv4Route.testBuilder()
            .setNetwork(Prefix.ZERO)
            .setNextHopIp(Ip.parse("10.0.0.1"))
            .setOriginatorIp(Ip.ZERO)
            .setOriginType(OriginType.INCOMPLETE)
            .setProtocol(RoutingProtocol.BGP)
            .build();
    Bgpv4Route deniedRoute = permittedRoute.toBuilder().setNextHopIp(Ip.parse("10.0.0.3")).build();

    Builder out_builder = org.batfish.datamodel.Bgpv4Route.builder();

    // Allowed from a specific neighbor
    assertThat(c.getRoutingPolicies(), hasKey("POL"));
    RoutingPolicy rp = c.getRoutingPolicies().get("POL");
    BgpSessionProperties session =
        BgpSessionProperties.builder()
            .setRemoteAs(65500)
            .setLocalAs(65501)
            .setRemoteIp(Ip.parse("10.0.0.1"))
            .setLocalIp(Ip.parse("10.9.9.9"))
            .build();
    assertTrue(rp.processBgpRoute(permittedRoute, out_builder, session, Direction.IN, null));
    assertFalse(rp.processBgpRoute(deniedRoute, out_builder, session, Direction.IN, null));
  }

  @Test
  public void testBgpDisable() throws IOException {
    // Config has "set protocols bgp disable"; no VI BGP process should be created
    String hostname = "bgp_disable";
    IBatfish iBatfish = getBatfishForConfigurationNames(hostname);
    Configuration c = iBatfish.loadConfigurations(iBatfish.getSnapshot()).get(hostname);
    assertThat(c.getVrfs().get(DEFAULT_VRF_NAME).getBgpProcess(), nullValue());
  }

  @Test
  public void testDefaultApplications() throws IOException {
    String hostname = "default-applications";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());
    Configuration c = parseConfig(hostname);

    String aclApplicationsName = zoneToZoneFilter("z1", "z2");
    String aclApplicationSetName = zoneToZoneFilter("z1", "z3");
    String aclApplicationSetAnyName = zoneToZoneFilter("z1", "z4");
    String aclApplicationAnyName = zoneToZoneFilter("z1", "z5");
    String z1Interface = "ge-0/0/0.0";
    IpAccessList aclApplication = c.getIpAccessLists().get(aclApplicationsName);
    IpAccessList aclApplicationSet = c.getIpAccessLists().get(aclApplicationSetName);
    IpAccessList aclApplicationSetAny = c.getIpAccessLists().get(aclApplicationSetAnyName);
    IpAccessList aclApplicationAny = c.getIpAccessLists().get(aclApplicationAnyName);
    /* Allowed applications permits TCP to port 80 and 443 */
    Flow permittedHttpFlow = createFlow(IpProtocol.TCP, 80);
    Flow permittedHttpsFlow = createFlow(IpProtocol.TCP, 443);
    Flow rejectedFlow = createFlow(IpProtocol.TCP, 100);

    /*
     * Confirm there are no undefined references
     */
    assertThat(ccae, hasNoUndefinedReferences());

    /*
     * Confirm acl with explicit application constraints accepts http and https flows and rejects
     * others
     */
    assertThat(
        aclApplication,
        accepts(permittedHttpFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        aclApplication,
        accepts(permittedHttpsFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        aclApplication, rejects(rejectedFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));

    /*
     * Confirm acl with indirect constraints (application-set) accepts http and https flows and
     * rejects others
     */
    assertThat(
        aclApplicationSet,
        accepts(permittedHttpFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        aclApplicationSet,
        accepts(permittedHttpsFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        aclApplicationSet,
        rejects(rejectedFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));

    /*
     * Confirm policy referencing permissive application-set accepts all three flows
     */
    assertThat(
        aclApplicationSetAny,
        accepts(permittedHttpFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        aclApplicationSetAny,
        accepts(permittedHttpsFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        aclApplicationSetAny,
        accepts(rejectedFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));

    /*
     * Confirm policy directly permitting any application accepts all three flows
     */
    assertThat(
        aclApplicationAny,
        accepts(permittedHttpFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        aclApplicationAny,
        accepts(permittedHttpsFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        aclApplicationAny,
        accepts(rejectedFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));
  }

  @Test
  public void testEnforceFirstAs() {
    String hostname = "bgp-enforce-first-as";
    Configuration c = parseConfig(hostname);
    assertThat(c, hasDefaultVrf(hasBgpProcess(hasNeighbors(hasValue(hasEnforceFirstAs())))));
  }

  @Test
  public void testEthernetSwitchingFilterReference() throws IOException {
    String hostname = "ethernet-switching-filter";
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    /* esfilter should be referred, while esfilter2 should be unreferred */
    assertThat(ccae, hasNumReferrers(filename, FIREWALL_FILTER, "esfilter", 1));
    assertThat(ccae, hasNumReferrers(filename, FIREWALL_FILTER, "esfilter2", 0));
  }

  @Test
  public void testSecurityZoneTermReference() throws IOException {
    String hostname = "security-zone-term-refs";
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    // Term is defined and has a self-reference
    assertThat(
        ccae,
        hasNumReferrers(
            filename,
            SECURITY_POLICY_TERM,
            computeSecurityPolicyTermName(getInboundFilterName("ZONE_NAME"), "ALL"),
            1));
  }

  @Test
  public void testEvpnEncapsulationExtraction() {
    JuniperConfiguration c = parseJuniperConfig("juniper-evpn-vxlan-encap");
    assertEquals(EvpnEncapsulation.VXLAN, c.getMasterLogicalSystem().getEvpn().getEncapsulation());
  }

  @Test
  public void testEvpnMulticastModeIngressExtraction() {
    JuniperConfiguration c = parseJuniperConfig("juniper-evpn-multicast-ingress");
    assertEquals(
        MulticastModeOptions.INGRESS_REPLICATION,
        c.getMasterLogicalSystem().getEvpn().getMulticastMode());
  }

  @Test
  public void testEvpnMulticastModeClientExtraction() {
    JuniperConfiguration c = parseJuniperConfig("juniper-evpn-multicast-client");
    assertEquals(
        MulticastModeOptions.CLIENT, c.getMasterLogicalSystem().getEvpn().getMulticastMode());
  }

  @Test
  public void testEvpnVniListAllExtraction() {
    JuniperConfiguration c = parseJuniperConfig("juniper-evpn-vni-list-all");
    assertTrue(c.getMasterLogicalSystem().getEvpn().getExtendedVniAll());
  }

  @Test
  public void testEvpnVniListNoRangeExtraction() {
    JuniperConfiguration c = parseJuniperConfig("juniper-evpn-vni-list-no-range");
    assertEquals(
        IntegerSpace.unionOf(
            IntegerSpace.of(10101), IntegerSpace.of(10103), IntegerSpace.of(10105)),
        c.getMasterLogicalSystem().getEvpn().getExtendedVniList());
  }

  @Test
  public void testEvpnVniListWithRangeExtraction() {
    JuniperConfiguration c = parseJuniperConfig("juniper-evpn-vni-list-with-range");
    assertEquals(
        IntegerSpace.unionOf(IntegerSpace.of(new SubRange(10101, 10103)), IntegerSpace.of(10105)),
        c.getMasterLogicalSystem().getEvpn().getExtendedVniList());
  }

  @Test
  public void testFirewallFilterReferences() throws IOException {
    String hostname = "firewall-filters";
    String filename = "configs/" + hostname;

    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    /* Confirm filter usage is tracked properly */
    assertThat(ccae, hasNumReferrers(filename, FIREWALL_FILTER, "FILTER1", 3));
    assertThat(ccae, hasNumReferrers(filename, FIREWALL_FILTER, "FILTER2", 4));
    assertThat(ccae, hasNumReferrers(filename, FIREWALL_FILTER, "FILTER_UNUSED", 0));

    /* Confirm undefined reference is identified */
    assertThat(ccae, hasUndefinedReference(filename, FIREWALL_FILTER, "FILTER_UNDEF"));

    /* Check interface and interface-set references */
    assertThat(ccae, hasNumReferrers(filename, FIREWALL_INTERFACE_SET, "ifset", 1));
    // 2 definition lines plus one usage in interface-set
    assertThat(ccae, hasNumReferrers(filename, INTERFACE, "xe-0/0/0.0", 3));
    // 2 definition lines plus one usage in firewall term "from interface"
    assertThat(ccae, hasNumReferrers(filename, INTERFACE, "xe-0/0/2.0", 3));

    /* Check no warnings other than firewall filter terms missing an action */
    assertThat(
        ccae.getWarnings().get(hostname).getRedFlagWarnings(),
        everyItem(WarningMatchers.hasText(containsString("missing action in firewall filter"))));
  }

  @Test
  public void testFirewallFilterExtraction() {
    JuniperConfiguration c = parseJuniperConfig("firewall-filters");
    Map<String, org.batfish.representation.juniper.Interface> ifaces =
        c.getMasterLogicalSystem().getInterfaces();

    {
      String parentName = "xe-0/0/0";
      String unitName = parentName + ".0";
      assertThat(ifaces, hasKey(parentName));
      org.batfish.representation.juniper.Interface parent = ifaces.get(parentName);
      assertThat(parent.getUnits(), hasKey(unitName));
      org.batfish.representation.juniper.Interface iface = parent.getUnits().get(unitName);
      assertThat(iface.getIncomingFilter(), equalTo("FILTER1"));
      assertThat(iface.getIncomingFilterList(), nullValue());
      assertThat(iface.getOutgoingFilter(), nullValue());
      assertThat(iface.getOutgoingFilterList(), nullValue());
    }
    {
      String parentName = "xe-0/0/1";
      String unitName = parentName + ".0";
      assertThat(ifaces, hasKey(parentName));
      org.batfish.representation.juniper.Interface parent = ifaces.get(parentName);
      assertThat(parent.getUnits(), hasKey(unitName));
      org.batfish.representation.juniper.Interface iface = parent.getUnits().get(unitName);
      assertThat(iface.getIncomingFilter(), equalTo("FILTER2"));
      assertThat(iface.getIncomingFilterList(), nullValue());
      assertThat(iface.getOutgoingFilter(), equalTo("FILTER2"));
      assertThat(iface.getOutgoingFilterList(), nullValue());
    }
    {
      String parentName = "xe-0/0/2";
      String unitName = parentName + ".0";
      assertThat(ifaces, hasKey(parentName));
      org.batfish.representation.juniper.Interface parent = ifaces.get(parentName);
      assertThat(parent.getUnits(), hasKey(unitName));
      org.batfish.representation.juniper.Interface iface = parent.getUnits().get(unitName);
      assertThat(iface.getIncomingFilter(), nullValue());
      assertThat(iface.getIncomingFilterList(), nullValue());
      assertThat(iface.getOutgoingFilter(), equalTo("FILTER_UNDEF"));
      assertThat(iface.getOutgoingFilterList(), nullValue());
    }
    {
      String parentName = "xe-0/0/3";
      String unitName = parentName + ".0";
      assertThat(ifaces, hasKey(parentName));
      org.batfish.representation.juniper.Interface parent = ifaces.get(parentName);
      assertThat(parent.getUnits(), hasKey(unitName));
      org.batfish.representation.juniper.Interface iface = parent.getUnits().get(unitName);
      assertThat(iface.getIncomingFilter(), nullValue());
      assertThat(iface.getIncomingFilterList(), contains("FILTER1", "FILTER2"));
      assertThat(iface.getOutgoingFilter(), nullValue());
      assertThat(iface.getOutgoingFilterList(), contains("FILTER2", "FILTER1"));
    }

    // filter definitions
    assertThat(c.getMasterLogicalSystem().getFirewallFilters(), hasKey("PARSING"));
    FirewallFilter f = c.getMasterLogicalSystem().getFirewallFilters().get("PARSING");
    assertThat(f, instanceOf(ConcreteFirewallFilter.class));
    ConcreteFirewallFilter cf = (ConcreteFirewallFilter) f;
    assertThat(
        cf.getTerms(),
        hasKeys(
            "FRAGMENT_OFFSET",
            "ICMP_TYPE",
            "ICMP_CODE",
            "INTERFACE_AND_INTERFACE_SET",
            "PACKET_LENGTH"));
    {
      FwTerm term = cf.getTerms().get("FRAGMENT_OFFSET");
      assertThat(term.getFroms(), hasSize(4));
      term.getFroms().forEach(from -> assertThat(from, instanceOf(FwFromFragmentOffset.class)));
      {
        FwFromFragmentOffset from = (FwFromFragmentOffset) term.getFroms().get(0);
        assertFalse(from.getExcept());
        assertThat(from.getOffsetRange(), equalTo(SubRange.singleton(1)));
      }
      {
        FwFromFragmentOffset from = (FwFromFragmentOffset) term.getFroms().get(1);
        assertFalse(from.getExcept());
        assertThat(from.getOffsetRange(), equalTo(new SubRange(3, 12)));
      }
      {
        FwFromFragmentOffset from = (FwFromFragmentOffset) term.getFroms().get(2);
        assertTrue(from.getExcept());
        assertThat(from.getOffsetRange(), equalTo(SubRange.singleton(5)));
      }
      {
        FwFromFragmentOffset from = (FwFromFragmentOffset) term.getFroms().get(3);
        assertTrue(from.getExcept());
        assertThat(from.getOffsetRange(), equalTo(new SubRange(10, 11)));
      }
    }
    {
      FwTerm term = cf.getTerms().get("ICMP_TYPE");
      assertThat(term.getFroms(), hasSize(4));
      assertThat(term.getFroms().get(0), instanceOf(FwFromIcmpType.class));
      assertThat(term.getFroms().get(1), instanceOf(FwFromIcmpType.class));
      assertThat(term.getFroms().get(2), instanceOf(FwFromIcmpTypeExcept.class));
      assertThat(term.getFroms().get(3), instanceOf(FwFromIcmpTypeExcept.class));
      {
        FwFromIcmpType from = (FwFromIcmpType) term.getFroms().get(0);
        assertThat(from.getIcmpTypeRange(), equalTo(SubRange.singleton(0)));
      }
      {
        FwFromIcmpType from = (FwFromIcmpType) term.getFroms().get(1);
        assertThat(from.getIcmpTypeRange(), equalTo(new SubRange(10, 20)));
      }
      {
        FwFromIcmpTypeExcept from = (FwFromIcmpTypeExcept) term.getFroms().get(2);
        assertThat(from.getIcmpTypeRange(), equalTo(SubRange.singleton(11)));
      }
      {
        FwFromIcmpTypeExcept from = (FwFromIcmpTypeExcept) term.getFroms().get(3);
        assertThat(from.getIcmpTypeRange(), equalTo(new SubRange(13, 14)));
      }
    }
    {
      FwTerm term = cf.getTerms().get("ICMP_CODE");
      assertThat(term.getFroms(), hasSize(4));
      assertThat(term.getFroms().get(0), instanceOf(FwFromIcmpCode.class));
      assertThat(term.getFroms().get(1), instanceOf(FwFromIcmpCode.class));
      assertThat(term.getFroms().get(2), instanceOf(FwFromIcmpCodeExcept.class));
      assertThat(term.getFroms().get(3), instanceOf(FwFromIcmpCodeExcept.class));
      {
        FwFromIcmpCode from = (FwFromIcmpCode) term.getFroms().get(0);
        assertThat(from.getIcmpCodeRange(), equalTo(SubRange.singleton(0)));
      }
      {
        FwFromIcmpCode from = (FwFromIcmpCode) term.getFroms().get(1);
        assertThat(from.getIcmpCodeRange(), equalTo(new SubRange(30, 40)));
      }
      {
        FwFromIcmpCodeExcept from = (FwFromIcmpCodeExcept) term.getFroms().get(2);
        assertThat(from.getIcmpCodeRange(), equalTo(SubRange.singleton(31)));
      }
      {
        FwFromIcmpCodeExcept from = (FwFromIcmpCodeExcept) term.getFroms().get(3);
        assertThat(from.getIcmpCodeRange(), equalTo(new SubRange(33, 34)));
      }
    }
    {
      FwTerm term = cf.getTerms().get("INTERFACE_AND_INTERFACE_SET");
      assertThat(term.getFroms(), hasSize(2));
      assertThat(term.getFroms().get(0), instanceOf(FwFromInterfaceSet.class));
      assertThat(term.getFroms().get(1), instanceOf(FwFromInterface.class));
      {
        FwFromInterfaceSet from = (FwFromInterfaceSet) term.getFroms().get(0);
        assertThat(from.getInterfaceSetName(), equalTo("ifset"));
      }
      {
        FwFromInterface from = (FwFromInterface) term.getFroms().get(1);
        assertThat(from.getInterfaceName(), equalTo("xe-0/0/2.0"));
      }
    }
    {
      FwTerm term = cf.getTerms().get("PACKET_LENGTH");
      assertThat(term.getFroms(), hasSize(4));
      term.getFroms().forEach(from -> assertThat(from, instanceOf(FwFromPacketLength.class)));
      {
        FwFromPacketLength from = (FwFromPacketLength) term.getFroms().get(0);
        assertFalse(from.getExcept());
        assertThat(from.getRange(), equalTo(SubRange.singleton(50)));
      }
      {
        FwFromPacketLength from = (FwFromPacketLength) term.getFroms().get(1);
        assertFalse(from.getExcept());
        assertThat(from.getRange(), equalTo(new SubRange(100, 200)));
      }
      {
        FwFromPacketLength from = (FwFromPacketLength) term.getFroms().get(2);
        assertTrue(from.getExcept());
        assertThat(from.getRange(), equalTo(SubRange.singleton(70)));
      }
      {
        FwFromPacketLength from = (FwFromPacketLength) term.getFroms().get(3);
        assertTrue(from.getExcept());
        assertThat(from.getRange(), equalTo(new SubRange(80, 90)));
      }
    }
  }

  @Test
  public void testFirewallFilterConversion() {
    Configuration c = parseConfig("firewall-filters");
    Flow.Builder fb = builder().setIpProtocol(OSPF).setIngressNode(c.getHostname()).setDstIp(ZERO);
    Flow src1235 = fb.setSrcIp(Ip.parse("1.2.3.5")).build();
    Flow src1236 = fb.setSrcIp(Ip.parse("1.2.3.6")).build();
    Flow src8888 = fb.setSrcIp(Ip.parse("8.8.8.8")).build();

    assertThat(
        c,
        hasIpAccessList(
            "xe-0/0/3.0-i",
            allOf(
                rejects(src1235, null, c), accepts(src1236, null, c), rejects(src8888, null, c))));
    assertThat(
        c,
        hasIpAccessList(
            "xe-0/0/3.0-o",
            allOf(
                rejects(src1235, null, c), rejects(src1236, null, c), rejects(src8888, null, c))));
  }

  @Test
  public void testTunnelAttributeExtraction() {
    JuniperConfiguration vc = parseJuniperConfig("tunnel-attributes");
    Map<String, TunnelAttribute> tunnelAttributes =
        vc.getMasterLogicalSystem().getTunnelAttributes();
    {
      TunnelAttribute tunnelAttr = tunnelAttributes.get("tunnel-attr-1");
      assertThat(tunnelAttr.getRemoteEndPoint(), equalTo(Ip.parse("1.2.3.4")));
      assertThat(tunnelAttr.getType(), equalTo(TunnelAttribute.Type.IPIP));
    }
    {
      TunnelAttribute tunnelAttr = tunnelAttributes.get("tunnel-attr-2");
      assertThat(tunnelAttr.getRemoteEndPoint(), equalTo(Ip.parse("5.6.7.8")));
      assertNull(tunnelAttr.getType());
    }
    {
      TunnelAttribute tunnelAttr = tunnelAttributes.get("tunnel-attr-3");
      assertNull(tunnelAttr.getRemoteEndPoint());
      assertThat(tunnelAttr.getType(), equalTo(TunnelAttribute.Type.IPIP));
    }
  }

  @Test
  public void testFirewallCombinedPolicies() {
    Configuration c = parseConfig("firewall-combined-policies");
    String interfaceNameTrust = "ge-0/0/0.0";
    String interfaceNameUntrust = "ge-0/0/1.0";
    String addrPermitted = "1.2.3.1";
    String addrDeniedByZonePolicy = "1.2.3.3";
    String addrDeniedByGlobalPolicy = "1.2.3.7";
    String addrDeniedByFilter = "2.2.2.2";
    String addrDefaultPolicy = "1.2.3.15";
    String untrustIpAddr = "1.2.4.5";

    Flow flowPermitted = createFlow(addrPermitted, untrustIpAddr);
    Flow flowDeniedByZonePolicy = createFlow(addrDeniedByZonePolicy, untrustIpAddr);
    Flow flowDeniedByGlobalPolicy = createFlow(addrDeniedByGlobalPolicy, untrustIpAddr);
    Flow flowDeniedByFilter = createFlow(addrDeniedByFilter, untrustIpAddr);
    Flow flowDefaultPolicy = createFlow(addrDefaultPolicy, untrustIpAddr);

    IpAccessList aclUntrustOut = c.getAllInterfaces().get(interfaceNameUntrust).getOutgoingFilter();
    IpAccessList aclUntrustSecurity =
        c.getAllInterfaces().get(interfaceNameUntrust).getPreTransformationOutgoingFilter();

    /* Confirm flow from address explicitly allowed by zone policy is accepted */
    assertThat(
        aclUntrustSecurity,
        accepts(flowPermitted, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
    /* Confirm flow from trust interface not matching any policy deny is accepted (accepted by default permit-all) */
    assertThat(
        aclUntrustSecurity,
        accepts(flowDefaultPolicy, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
    /* Confirm flow matching zone policy deny is rejected */
    assertThat(
        aclUntrustSecurity,
        rejects(flowDeniedByZonePolicy, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
    /* Confirm flow blocked by the outgoing filter is rejected */
    assertThat(
        aclUntrustOut,
        rejects(flowDeniedByFilter, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
    /* Confirm flow matching global policy deny is rejected */
    assertThat(
        aclUntrustSecurity,
        rejects(
            flowDeniedByGlobalPolicy, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));

    /* Confirm traffic originating from the device is not blocked by policies */
    assertThat(
        aclUntrustSecurity,
        accepts(flowDeniedByZonePolicy, null, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        aclUntrustSecurity,
        accepts(flowDeniedByGlobalPolicy, null, c.getIpAccessLists(), c.getIpSpaces()));
    /* Confirm traffic originating from the device is still blocked by an outgoing filter */
    assertThat(
        aclUntrustOut, rejects(flowDeniedByFilter, null, c.getIpAccessLists(), c.getIpSpaces()));
  }

  @Test
  public void testFirewallGlobalAddressBook() {
    Configuration c = parseConfig("firewall-global-address-book");
    String interfaceNameTrust = "ge-0/0/0.0";
    String interfaceNameUntrust = "ge-0/0/1.0";
    String specificSpaceName = "global~ADDR1";
    String wildcardSpaceName = "global~ADDR2";
    String rangeSpaceName = "global~ADDR3";
    String indirectSpaceName = "global~ADDRSET";

    // Address on untrust interface's subnet
    String untrustIpAddr = "1.2.4.5";
    // Specific address allowed by the address-set
    String specificAddr = "2.2.2.2";
    // Address allowed by the wildcard-address in the address-set
    String wildcardAddr = "1.3.3.4";
    // Address not allowed by either entry in the address-set
    String notWildcardAddr = "1.2.3.5";

    Flow flowFromSpecificAddr = createFlow(specificAddr, untrustIpAddr);
    Flow flowFromWildcardAddr = createFlow(wildcardAddr, untrustIpAddr);
    Flow flowFromNotWildcardAddr = createFlow(notWildcardAddr, untrustIpAddr);
    IpAccessList untrustAcl =
        c.getAllInterfaces().get(interfaceNameUntrust).getPreTransformationOutgoingFilter();

    // Should have three global IpSpaces in the config
    assertThat(
        c.getIpSpaces().keySet(),
        containsInAnyOrder(
            specificSpaceName, wildcardSpaceName, indirectSpaceName, rangeSpaceName));
    // And associated metadata
    assertThat(
        c.getIpSpaceMetadata().keySet(),
        containsInAnyOrder(
            specificSpaceName, wildcardSpaceName, indirectSpaceName, rangeSpaceName));

    IpSpace specificSpace = c.getIpSpaces().get(specificSpaceName);
    IpSpace wildcardSpace = c.getIpSpaces().get(wildcardSpaceName);
    IpSpace indirectSpace = c.getIpSpaces().get(indirectSpaceName);

    // Specific space should contain the specific addr and not others
    assertThat(specificSpace, containsIp(Ip.parse(specificAddr)));
    assertThat(specificSpace, not(containsIp(Ip.parse(wildcardAddr))));

    // Wildcard space should contain the wildcard addr and not others
    assertThat(wildcardSpace, containsIp(Ip.parse(wildcardAddr)));
    assertThat(wildcardSpace, not(containsIp(Ip.parse(notWildcardAddr))));

    // Indirect space should contain both specific and wildcard addr, but not others
    assertThat(indirectSpace, containsIp(Ip.parse(specificAddr), c.getIpSpaces()));
    assertThat(indirectSpace, containsIp(Ip.parse(wildcardAddr), c.getIpSpaces()));
    assertThat(indirectSpace, not(containsIp(Ip.parse(notWildcardAddr), c.getIpSpaces())));

    // Specifically allowed source addr should be accepted
    assertThat(
        untrustAcl,
        accepts(flowFromSpecificAddr, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
    // Source addr covered by the wildcard entry should be accepted
    assertThat(
        untrustAcl,
        accepts(flowFromWildcardAddr, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
    // Source addr covered by neither addr-set entry should be rejected
    assertThat(
        untrustAcl,
        rejects(
            flowFromNotWildcardAddr, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));

    // Range
    IpSpace rangeSpace = c.getIpSpaces().get(rangeSpaceName);
    Ip before = Ip.parse("5.5.5.4");
    Ip min = Ip.parse("5.5.5.5");
    Ip inside = Ip.parse("5.5.5.6");
    Ip max = Ip.parse("5.5.5.7");
    Ip after = Ip.parse("5.5.5.8");
    assertThat(
        rangeSpace,
        allOf(
            not(containsIp(before)),
            containsIp(min),
            containsIp(inside),
            containsIp(max),
            not(containsIp(after))));
  }

  @Test
  public void testFirewallGlobalAddressBookRangeError() throws IOException {
    String hostname = "firewall-global-address-book-range-error";
    String fileKey = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    batfish.loadConfigurations(batfish.getSnapshot());
    ParseVendorConfigurationAnswerElement pvcae =
        batfish.loadParseVendorConfigurationAnswerElement(batfish.getSnapshot());

    assertThat(
        pvcae.getWarnings().get(fileKey).getFatalRedFlagWarnings(),
        hasItem(
            WarningMatchers.hasText(
                containsString(
                    "Range must be from low to high: address INVALID range-address 5.5.5.7 to"
                        + " 5.5.5.5"))));
  }

  @Test
  public void testFirewallGlobalPolicy() {
    Configuration c = parseConfig("firewall-global-policy");
    String interfaceNameTrust = "ge-0/0/0.0";
    String interfaceNameUntrust = "ge-0/0/1.0";
    String trustedIpAddr = "1.2.3.5";
    String untrustedIpAddr = "1.2.4.5";

    Flow trustToUntrustFlow = createFlow(trustedIpAddr, untrustedIpAddr);
    Flow untrustToTrustFlow = createFlow(untrustedIpAddr, trustedIpAddr);

    IpAccessList aclTrustOut =
        c.getAllInterfaces().get(interfaceNameTrust).getPreTransformationOutgoingFilter();
    IpAccessList aclUntrustOut =
        c.getAllInterfaces().get(interfaceNameUntrust).getPreTransformationOutgoingFilter();

    /*
     * Should have six ACLs:
     *  Explicitly defined in the config file:
     *    One from the global security policy
     *  Generated by logic in toVendorIndependent
     *    One permitting existing connections (default firewall behavior)
     *    Two defining security policies for each interface (combines explicit security policy with
     *        implicit security policies like allow existing connection)
     *    Two inbound policies, one for each zone
     */
    assertThat(
        c.getIpAccessLists().keySet(),
        containsInAnyOrder(
            ACL_NAME_GLOBAL_POLICY,
            ACL_NAME_SECURITY_POLICY + interfaceNameTrust,
            ACL_NAME_SECURITY_POLICY + interfaceNameUntrust,
            getInboundFilterName("trust"),
            getInboundFilterName("untrust")));

    /* Flows in either direction should be permitted by the global policy */
    assertThat(
        aclUntrustOut,
        accepts(trustToUntrustFlow, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        aclTrustOut,
        accepts(untrustToTrustFlow, interfaceNameUntrust, c.getIpAccessLists(), c.getIpSpaces()));
  }

  @Test
  public void testFirewallGlobalPolicyGlobalAddressBook() {
    /*
     * Test address book behavior when used in a global policy
     * i.e. a policy that does not have fromZone or toZone
     */
    Configuration c = parseConfig("firewall-global-policy-global-address-book");
    String interfaceNameTrust = "ge-0/0/0.0";
    String interfaceNameUntrust = "ge-0/0/1.0";
    String trustedIpAddr = "1.2.3.5";
    String untrustedIpAddr = "1.2.4.5";
    String trustedSpaceName = "global~ADDR1";

    Flow trustToUntrustFlow = createFlow(trustedIpAddr, untrustedIpAddr);
    Flow untrustToTrustFlow = createFlow(untrustedIpAddr, trustedIpAddr);

    IpAccessList aclTrustOut =
        c.getAllInterfaces().get(interfaceNameTrust).getPreTransformationOutgoingFilter();
    IpAccessList aclUntrustOut =
        c.getAllInterfaces().get(interfaceNameUntrust).getPreTransformationOutgoingFilter();

    /* Make sure the global-address-book address is the only config ipSpace */
    assertThat(c.getIpSpaces().keySet(), containsInAnyOrder(trustedSpaceName));

    IpSpace ipSpace = getOnlyElement(c.getIpSpaces().values());

    // It should contain the specific address
    assertThat(ipSpace, containsIp(Ip.parse(trustedIpAddr)));

    // It should not contain the address that is not allowed
    assertThat(ipSpace, not(containsIp(Ip.parse(untrustedIpAddr))));

    /* Flow from ADDR1 to untrust should be permitted */
    assertThat(
        aclUntrustOut,
        accepts(trustToUntrustFlow, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
    /* Flow from not ADDR1 to trust should be rejected */
    assertThat(
        aclTrustOut,
        rejects(untrustToTrustFlow, interfaceNameUntrust, c.getIpAccessLists(), c.getIpSpaces()));
  }

  @Test
  public void testFirewallNoPolicies() {
    Configuration c = parseConfig("firewall-no-policies");
    String interfaceNameTrust = "ge-0/0/0.0";
    String interfaceNameUntrust = "ge-0/0/1.0";
    String trustedIpAddr = "1.2.3.5";
    String untrustedIpAddr = "1.2.4.5";

    Flow trustToUntrustFlow = createFlow(trustedIpAddr, untrustedIpAddr);
    Flow untrustToTrustFlow = createFlow(untrustedIpAddr, trustedIpAddr);

    IpAccessList aclTrustOut =
        c.getAllInterfaces().get(interfaceNameTrust).getPreTransformationOutgoingFilter();
    IpAccessList aclUntrustOut =
        c.getAllInterfaces().get(interfaceNameUntrust).getPreTransformationOutgoingFilter();

    /*
     * Should have four ACLs generated by logic in toVendorIndependent:
     *    Two defining security policies for each interface (combines explicit security policy with
     *        implicit security policies like allow existing connection)
     *    Two for host-inbound-protocols
     */
    assertThat(
        c.getIpAccessLists().keySet(),
        containsInAnyOrder(
            ACL_NAME_SECURITY_POLICY + interfaceNameTrust,
            ACL_NAME_SECURITY_POLICY + interfaceNameUntrust,
            getInboundFilterName("trust"),
            getInboundFilterName("untrust")));

    /* Simple flow in either direction should be blocked */
    assertThat(
        aclUntrustOut,
        rejects(trustToUntrustFlow, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        aclTrustOut,
        rejects(untrustToTrustFlow, interfaceNameUntrust, c.getIpAccessLists(), c.getIpSpaces()));
  }

  @Test
  public void testFirewallApplicationServicesExtraction() {
    String hostname = "firewall-application-services";
    JuniperConfiguration vc = parseJuniperConfig(hostname);
    assertThat(
        vc.getMasterLogicalSystem()
            .getSecurityPolicies()
            .get(zoneToZoneFilter("trust", "untrust"))
            .getTerms()
            .get("PNAME")
            .getThens()
            .get(0),
        instanceOf(FwThenAccept.class));
  }

  @Test
  public void testFirewallPolicies() {
    Configuration c = parseConfig("firewall-policies");
    String interfaceNameTrust = "ge-0/0/0.0";
    String interfaceNameUntrust = "ge-0/0/1.0";
    String securityPolicyName = zoneToZoneFilter("trust", "untrust");
    String trustedIpAddr = "1.2.3.5";
    String untrustedIpAddr = "1.2.4.5";

    Flow trustToUntrustFlow = createFlow(trustedIpAddr, untrustedIpAddr);
    Flow untrustToTrustFlow = createFlow(untrustedIpAddr, trustedIpAddr);

    IpAccessList aclTrustOut =
        c.getAllInterfaces().get(interfaceNameTrust).getPreTransformationOutgoingFilter();
    IpAccessList aclUntrustOut =
        c.getAllInterfaces().get(interfaceNameUntrust).getPreTransformationOutgoingFilter();

    /*
     * Should have six ACLs:
     *  Explicitly defined in the config file:
     *    One from the security policy from trust to untrust
     *  Generated by logic in toVendorIndependent
     *    Two defining security policies for each interface (combines explicit security policy with
     *        implicit security policies like allow existing connection)
     *    Two for host-inbound-protocols
     */
    assertThat(
        c.getIpAccessLists().keySet(),
        containsInAnyOrder(
            securityPolicyName,
            "~" + securityPolicyName + "~pure",
            aclTrustOut.getName(),
            aclUntrustOut.getName(),
            getInboundFilterName("trust"),
            getInboundFilterName("untrust")));

    /* Simple flow from trust to untrust should be permitted */
    assertThat(
        aclUntrustOut,
        accepts(trustToUntrustFlow, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));

    /* Simple flow from untrust to trust should be blocked */
    assertThat(
        aclTrustOut,
        rejects(untrustToTrustFlow, interfaceNameUntrust, c.getIpAccessLists(), c.getIpSpaces()));
  }

  @Test
  public void testFirewallZoneAddressBookInline() {
    Configuration c = parseConfig("firewall-zone-address-book-inline");
    String interfaceNameTrust = "ge-0/0/0.0";
    String interfaceNameUntrust = "ge-0/0/1.0";
    // Address on untrust interface's subnet
    String untrustIpAddr = "1.2.4.5";
    // Specific address allowed by the address-book
    String specificAddr = "2.2.2.2";
    // Address not allowed by the address-book
    String notAllowedAddr = "3.3.3.3";

    Flow flowFromSpecificAddr = createFlow(specificAddr, untrustIpAddr);
    Flow flowFromNotAllowedAddr = createFlow(notAllowedAddr, untrustIpAddr);

    IpAccessList aclUntrustOut =
        c.getAllInterfaces().get(interfaceNameUntrust).getPreTransformationOutgoingFilter();

    // Should have a an IpSpace in the config corresponding to the trust zone's ADDR1 address
    String ipSpaceName = "trust~ADDR1";
    assertThat(c.getIpSpaces(), hasKey(equalTo(ipSpaceName)));

    // It should be the only IpSpace
    assertThat(c.getIpSpaces().keySet(), iterableWithSize(1));
    IpSpace ipSpace = getOnlyElement(c.getIpSpaces().values());

    // It should contain the specific address
    assertThat(ipSpace, containsIp(Ip.parse(specificAddr)));

    // It should not contain the address that is not allowed
    assertThat(ipSpace, not(containsIp(Ip.parse(notAllowedAddr))));

    // There should be metadata for this ipspace
    assertThat(c.getIpSpaceMetadata(), hasKey(ipSpaceName));

    // Specifically allowed source address should be accepted
    assertThat(
        aclUntrustOut,
        accepts(flowFromSpecificAddr, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
    // Source address not covered by the address-book should be rejected
    assertThat(
        aclUntrustOut,
        rejects(flowFromNotAllowedAddr, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
  }

  /**
   * Similar test to {@link #testFirewallZoneAddressBookInline()} except with attached instead of
   * inlined address book
   */
  @Test
  public void testFirewallZoneAddressBookAttach() {
    Configuration c = parseConfig("firewall-zone-address-book-attach");
    String interfaceNameTrust = "ge-0/0/0.0";
    String interfaceNameUntrust = "ge-0/0/1.0";
    // Address on untrust interface's subnet
    String untrustIpAddr = "1.2.4.5";
    // Specific address allowed by the address-book
    String specificAddr = "2.2.2.2";
    // Address not allowed by the address-book
    String notAllowedAddr = "3.3.3.3";

    Flow flowFromSpecificAddr = createFlow(specificAddr, untrustIpAddr);
    Flow flowFromNotAllowedAddr = createFlow(notAllowedAddr, untrustIpAddr);

    IpAccessList aclUntrustOut =
        c.getAllInterfaces().get(interfaceNameUntrust).getPreTransformationOutgoingFilter();

    // Should have a an IpSpace in the config corresponding to the trust zone's ADDR1 address
    String ipSpaceName = "trust-book~ADDR1";
    assertThat(c.getIpSpaces(), hasKey(equalTo(ipSpaceName)));

    // It should be the only IpSpace
    assertThat(c.getIpSpaces().keySet(), iterableWithSize(1));
    IpSpace ipSpace = getOnlyElement(c.getIpSpaces().values());

    // It should contain the specific address
    assertThat(ipSpace, containsIp(Ip.parse(specificAddr)));

    // It should not contain the address that is not allowed
    assertThat(ipSpace, not(containsIp(Ip.parse(notAllowedAddr))));

    // There should be metadata for this ipspace
    assertThat(c.getIpSpaceMetadata(), hasKey(ipSpaceName));

    // Specifically allowed source address should be accepted
    assertThat(
        aclUntrustOut,
        accepts(flowFromSpecificAddr, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
    // Source address not covered by the address-book should be rejected
    assertThat(
        aclUntrustOut,
        rejects(flowFromNotAllowedAddr, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
  }

  /**
   * Similar test to {@link #testFirewallZoneAddressBookInline()} except with attached instead of
   * inlined address book
   */
  @Test
  public void testFirewallZoneAddressBookGlobal() {
    Configuration c = parseConfig("firewall-zone-address-book-global");
    String interfaceNameTrust = "ge-0/0/0.0";
    String interfaceNameUntrust = "ge-0/0/1.0";
    // Address on untrust interface's subnet
    String untrustIpAddr = "1.2.4.5";
    // Specific address allowed by the address-book
    String specificAddr = "2.2.2.2";
    // Address not allowed by the address-book
    String notAllowedAddr = "3.3.3.3";

    Flow flowFromSpecificAddr = createFlow(specificAddr, untrustIpAddr);
    Flow flowFromNotAllowedAddr = createFlow(notAllowedAddr, untrustIpAddr);

    IpAccessList aclUntrustOut =
        c.getAllInterfaces().get(interfaceNameUntrust).getPreTransformationOutgoingFilter();

    // Should have a an IpSpace in the config corresponding to the trust zone's ADDR1 address
    String ipSpaceName = "global~ADDR1";
    assertThat(c.getIpSpaces(), hasKey(equalTo(ipSpaceName)));

    // It should be the only IpSpace
    assertThat(c.getIpSpaces().keySet(), iterableWithSize(1));
    IpSpace ipSpace = getOnlyElement(c.getIpSpaces().values());

    // It should contain the specific address
    assertThat(ipSpace, containsIp(Ip.parse(specificAddr)));

    // It should not contain the address that is not allowed
    assertThat(ipSpace, not(containsIp(Ip.parse(notAllowedAddr))));

    // There should be metadata for this ipspace
    assertThat(c.getIpSpaceMetadata(), hasKey(ipSpaceName));

    // Specifically allowed source address should be accepted
    assertThat(
        aclUntrustOut,
        accepts(flowFromSpecificAddr, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
    // Source address not covered by the address-book should be rejected
    assertThat(
        aclUntrustOut,
        rejects(flowFromNotAllowedAddr, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
  }

  /**
   * When both global and zone-specific (attached or inline) address books are present, global loses
   */
  @Test
  public void testFirewallZoneAddressBookGlobalLoses() {
    Configuration c = parseConfig("firewall-zone-address-book-global-loses");
    String interfaceNameTrust = "ge-0/0/0.0";
    String interfaceNameUntrust = "ge-0/0/1.0";
    // Address on untrust interface's subnet
    String untrustIpAddr = "1.2.4.5";
    // Specific address allowed by the address-book
    String specificAddr = "2.2.2.2";
    // Address not allowed by the address-book
    String notAllowedAddr = "3.3.3.3";

    Flow flowFromSpecificAddr = createFlow(specificAddr, untrustIpAddr);
    Flow flowFromNotAllowedAddr = createFlow(notAllowedAddr, untrustIpAddr);

    IpAccessList aclUntrustOut =
        c.getAllInterfaces().get(interfaceNameUntrust).getPreTransformationOutgoingFilter();

    // Specifically allowed source address should be accepted
    assertThat(
        aclUntrustOut,
        accepts(flowFromSpecificAddr, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
    // Source address not covered by the zone address-book should be rejected
    assertThat(
        aclUntrustOut,
        rejects(flowFromNotAllowedAddr, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
  }

  @Test
  public void testFirewallZoneAddressUndefined() {
    Configuration c = parseConfig("firewall-zone-address-undefined");

    String interfaceNameTrust = "ge-0/0/0.0";
    String interfaceNameUntrust = "ge-0/0/1.0";
    String addrAccepted = "2.2.2.2";
    String addrRejected = "2.2.2.3";

    Flow flowAccepted = createFlow(addrAccepted, addrAccepted);
    Flow flowRejected = createFlow(addrAccepted, addrRejected);

    IpAccessList aclUntrust =
        c.getAllInterfaces().get(interfaceNameUntrust).getPreTransformationOutgoingFilter();
    IpAccessList aclTrust =
        c.getAllInterfaces().get(interfaceNameTrust).getPreTransformationOutgoingFilter();

    // Make sure flow matching address-book entry is accepted despite the rule having one undefined
    // destination address
    assertThat(
        aclUntrust,
        accepts(flowAccepted, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
    // Make sure flow not matching address-book entry is rejected
    assertThat(
        aclUntrust,
        rejects(flowRejected, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));

    // Make sure both flows are rejected by rule with no defined destination address
    assertThat(
        aclTrust,
        rejects(flowAccepted, interfaceNameUntrust, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        aclTrust,
        rejects(flowRejected, interfaceNameUntrust, c.getIpAccessLists(), c.getIpSpaces()));
  }

  @Test
  public void testFirewallZoneAddressBookAttachAndGlobal() {
    Configuration c = parseConfig("firewall-zone-address-book-attach-and-global");

    String interfaceNameTrust = "ge-0/0/0.0";
    String interfaceNameUntrust = "ge-0/0/1.0";
    // Destination address allowed by the address-book
    String destAddr = "2.2.2.2";
    // Source address allowed by the address-book
    String sourceAddr = "3.3.3.3";

    Flow flowAllowed = createFlow(sourceAddr, destAddr);
    Flow flowRejected1 = createFlow(destAddr, destAddr);
    Flow flowRejected2 = createFlow(sourceAddr, sourceAddr);

    IpAccessList acl =
        c.getAllInterfaces().get(interfaceNameUntrust).getPreTransformationOutgoingFilter();

    // Confirm both global and attached address-book entries are processed properly
    // Make sure the flow with source address matching global book and destination address matching
    // attached book is accepted
    assertThat(
        acl, accepts(flowAllowed, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
    // Make sure flow with different addresses is denied
    assertThat(
        acl, rejects(flowRejected1, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        acl, rejects(flowRejected2, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
  }

  @Test
  public void testFirewallZones() {
    Configuration c = parseConfig("firewall-no-policies");
    String interfaceNameTrust = "ge-0/0/0.0";
    String interfaceNameUntrust = "ge-0/0/1.0";
    String zoneTrust = "trust";
    String zoneUntrust = "untrust";
    String trustedIpAddr = "1.2.3.5";
    String untrustedIpAddr = "1.2.4.5";

    Flow trustToUntrustFlow = createFlow(trustedIpAddr, untrustedIpAddr);
    Flow untrustToTrustFlow = createFlow(untrustedIpAddr, trustedIpAddr);

    IpAccessList aclTrustOut =
        c.getAllInterfaces().get(interfaceNameTrust).getPreTransformationOutgoingFilter();
    IpAccessList aclUntrustOut =
        c.getAllInterfaces().get(interfaceNameUntrust).getPreTransformationOutgoingFilter();

    // Should have two zones
    assertThat(c.getZones().keySet(), containsInAnyOrder(zoneTrust, zoneUntrust));

    // Should have two logical interfaces
    assertThat(c.getAllInterfaces().keySet(), hasItems(interfaceNameTrust, interfaceNameUntrust));

    // Confirm the interfaces are associated with their zones
    assertThat(c.getAllInterfaces().get(interfaceNameTrust), hasZoneName(equalTo(zoneTrust)));
    assertThat(c.getAllInterfaces().get(interfaceNameUntrust), hasZoneName(equalTo(zoneUntrust)));

    /* Simple flows should be blocked */
    assertThat(
        aclUntrustOut,
        rejects(trustToUntrustFlow, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        aclTrustOut,
        rejects(untrustToTrustFlow, interfaceNameUntrust, c.getIpAccessLists(), c.getIpSpaces()));
  }

  @Test
  public void testNameServer() {
    Configuration config = parseConfig("name-server");
    assertThat(config.getDnsServers(), contains("1.2.3.4", "2.0.0.0"));
  }

  @Test
  public void testAggregateDefaults() {
    Configuration config = parseConfig("aggregate-defaults");

    Set<GeneratedRoute> aggregateRoutes = config.getDefaultVrf().getGeneratedRoutes();
    GeneratedRoute ar1 =
        aggregateRoutes.stream()
            .filter(ar -> ar.getNetwork().equals(Prefix.parse("1.0.0.0/8")))
            .findAny()
            .get();
    GeneratedRoute ar2 =
        aggregateRoutes.stream()
            .filter(ar -> ar.getNetwork().equals(Prefix.parse("2.0.0.0/8")))
            .findAny()
            .get();
    GeneratedRoute ar3 =
        aggregateRoutes.stream()
            .filter(ar -> ar.getNetwork().equals(Prefix.parse("3.0.0.0/8")))
            .findAny()
            .get();

    // passive default
    // policies should be generated only for the active ones
    assertThat(ar1.getGenerationPolicy(), nullValue());
    assertThat(
        ar2.getGenerationPolicy(),
        equalTo(
            JuniperConfiguration.computeAggregatedRouteGenerationPolicyName(
                Prefix.parse("2.0.0.0/8"))));
    assertThat(ar3.getGenerationPolicy(), nullValue());

    Set<GeneratedRoute> aggregateRoutesRi1 = config.getVrfs().get("ri1").getGeneratedRoutes();
    GeneratedRoute ar1Ri1 =
        aggregateRoutesRi1.stream()
            .filter(ar -> ar.getNetwork().equals(Prefix.parse("1.0.0.0/8")))
            .findAny()
            .get();
    GeneratedRoute ar2Ri1 =
        aggregateRoutesRi1.stream()
            .filter(ar -> ar.getNetwork().equals(Prefix.parse("2.0.0.0/8")))
            .findAny()
            .get();
    GeneratedRoute ar3Ri1 =
        aggregateRoutesRi1.stream()
            .filter(ar -> ar.getNetwork().equals(Prefix.parse("3.0.0.0/8")))
            .findAny()
            .get();

    // active default
    // policies should be generated only for the active ones
    assertThat(
        ar1Ri1.getGenerationPolicy(),
        equalTo(
            JuniperConfiguration.computeAggregatedRouteGenerationPolicyName(
                Prefix.parse("1.0.0.0/8"))));
    assertThat(
        ar2Ri1.getGenerationPolicy(),
        equalTo(
            JuniperConfiguration.computeAggregatedRouteGenerationPolicyName(
                Prefix.parse("2.0.0.0/8"))));
    assertThat(ar3Ri1.getGenerationPolicy(), nullValue());
  }

  @Test
  public void testAggregateRoutesGenerationPolicies() {
    Configuration config = parseConfig("aggregate-routes");

    Set<GeneratedRoute> aggregateRoutes = config.getDefaultVrf().getGeneratedRoutes();
    GeneratedRoute ar1 =
        aggregateRoutes.stream()
            .filter(ar -> ar.getNetwork().equals(Prefix.parse("1.0.0.0/8")))
            .findAny()
            .get();
    GeneratedRoute ar2 =
        aggregateRoutes.stream()
            .filter(ar -> ar.getNetwork().equals(Prefix.parse("2.0.0.0/8")))
            .findAny()
            .get();
    GeneratedRoute ar3 =
        aggregateRoutes.stream()
            .filter(ar -> ar.getNetwork().equals(Prefix.parse("3.0.0.0/8")))
            .findAny()
            .get();
    GeneratedRoute ar4 =
        aggregateRoutes.stream()
            .filter(ar -> ar.getNetwork().equals(Prefix.parse("4.0.0.0/8")))
            .findAny()
            .get();
    GeneratedRoute ar5 =
        aggregateRoutes.stream()
            .filter(ar -> ar.getNetwork().equals(Prefix.parse("5.0.0.0/8")))
            .findAny()
            .get();
    GeneratedRoute ar6 =
        aggregateRoutes.stream()
            .filter(ar -> ar.getNetwork().equals(Prefix.parse("6.0.0.0/8")))
            .findAny()
            .get();

    // policies should be generated only for the active ones
    assertThat(
        ar1.getGenerationPolicy(),
        equalTo(
            JuniperConfiguration.computeAggregatedRouteGenerationPolicyName(
                Prefix.parse("1.0.0.0/8"))));
    assertThat(
        ar2.getGenerationPolicy(),
        equalTo(
            JuniperConfiguration.computeAggregatedRouteGenerationPolicyName(
                Prefix.parse("2.0.0.0/8"))));
    assertThat(ar3.getGenerationPolicy(), nullValue());

    // the second one should only accept 2.0.0.0/32 as a contributor
    RoutingPolicy rp2 = config.getRoutingPolicies().get(ar2.getGenerationPolicy());
    ConnectedRoute cr31 = new ConnectedRoute(Prefix.parse("2.0.0.0/31"), "blah");
    ConnectedRoute cr32 = new ConnectedRoute(Prefix.parse("2.0.0.0/32"), "blah");
    assertThat(rp2.process(cr31, Bgpv4Route.testBuilder(), Direction.OUT), equalTo(false));
    assertThat(rp2.process(cr32, Bgpv4Route.testBuilder(), Direction.OUT), equalTo(true));

    // all should be discard routes
    assertThat(ar1.getDiscard(), equalTo(true));
    assertThat(ar2.getDiscard(), equalTo(true));
    assertThat(ar3.getDiscard(), equalTo(true));

    // policy semantics

    // falls through without changing default, so accept
    RoutingPolicy rp4 = config.getRoutingPolicies().get(ar4.getGenerationPolicy());
    ConnectedRoute cr4 = new ConnectedRoute(Prefix.parse("4.0.0.0/32"), "blah");
    assertThat(rp4.process(cr4, Bgpv4Route.testBuilder(), Direction.OUT), equalTo(true));

    // rejects first, so reject
    RoutingPolicy rp5 = config.getRoutingPolicies().get(ar5.getGenerationPolicy());
    ConnectedRoute cr5 = new ConnectedRoute(Prefix.parse("5.0.0.0/32"), "blah");
    assertThat(rp5.process(cr5, Bgpv4Route.testBuilder(), Direction.OUT), equalTo(false));

    // accepts first, so accept
    RoutingPolicy rp6 = config.getRoutingPolicies().get(ar6.getGenerationPolicy());
    ConnectedRoute cr6 = new ConnectedRoute(Prefix.parse("6.0.0.0/32"), "blah");
    assertThat(rp6.process(cr6, Bgpv4Route.testBuilder(), Direction.OUT), equalTo(true));
  }

  @Test
  public void testGeneratedDefaults() {
    Configuration config = parseConfig("generated-defaults");

    Set<GeneratedRoute> generatedRoutes = config.getDefaultVrf().getGeneratedRoutes();
    GeneratedRoute gr1 =
        generatedRoutes.stream()
            .filter(gr -> gr.getNetwork().equals(Prefix.parse("1.0.0.0/8")))
            .findAny()
            .get();
    GeneratedRoute gr2 =
        generatedRoutes.stream()
            .filter(gr -> gr.getNetwork().equals(Prefix.parse("2.0.0.0/8")))
            .findAny()
            .get();
    GeneratedRoute gr3 =
        generatedRoutes.stream()
            .filter(gr -> gr.getNetwork().equals(Prefix.parse("3.0.0.0/8")))
            .findAny()
            .get();

    // passive default
    // policies should be generated only for the active ones
    assertThat(gr1.getGenerationPolicy(), nullValue());
    assertThat(
        gr2.getGenerationPolicy(),
        equalTo(
            JuniperConfiguration.computeGeneratedRouteGenerationPolicyName(
                Prefix.parse("2.0.0.0/8"))));
    assertThat(gr3.getGenerationPolicy(), nullValue());

    Set<GeneratedRoute> generatedRi1 = config.getVrfs().get("ri1").getGeneratedRoutes();
    GeneratedRoute gr1Ri1 =
        generatedRi1.stream()
            .filter(gr -> gr.getNetwork().equals(Prefix.parse("1.0.0.0/8")))
            .findAny()
            .get();
    GeneratedRoute gr2Ri1 =
        generatedRi1.stream()
            .filter(gr -> gr.getNetwork().equals(Prefix.parse("2.0.0.0/8")))
            .findAny()
            .get();
    GeneratedRoute gr3Ri1 =
        generatedRi1.stream()
            .filter(gr -> gr.getNetwork().equals(Prefix.parse("3.0.0.0/8")))
            .findAny()
            .get();

    // active default
    // policies should be generated only for the active ones
    assertThat(
        gr1Ri1.getGenerationPolicy(),
        equalTo(
            JuniperConfiguration.computeGeneratedRouteGenerationPolicyName(
                Prefix.parse("1.0.0.0/8"))));
    assertThat(
        gr2Ri1.getGenerationPolicy(),
        equalTo(
            JuniperConfiguration.computeGeneratedRouteGenerationPolicyName(
                Prefix.parse("2.0.0.0/8"))));
    assertThat(gr3Ri1.getGenerationPolicy(), nullValue());
  }

  @Test
  public void testGeneratedRoutesGenerationPolicies() {
    Configuration config = parseConfig("generated-routes");

    Set<GeneratedRoute> generatedRoutes = config.getDefaultVrf().getGeneratedRoutes();
    GeneratedRoute gr1 =
        generatedRoutes.stream()
            .filter(gr -> gr.getNetwork().equals(Prefix.parse("1.0.0.0/8")))
            .findAny()
            .get();
    GeneratedRoute gr2 =
        generatedRoutes.stream()
            .filter(gr -> gr.getNetwork().equals(Prefix.parse("2.0.0.0/8")))
            .findAny()
            .get();
    GeneratedRoute gr3 =
        generatedRoutes.stream()
            .filter(gr -> gr.getNetwork().equals(Prefix.parse("3.0.0.0/8")))
            .findAny()
            .get();
    GeneratedRoute gr4 =
        generatedRoutes.stream()
            .filter(gr -> gr.getNetwork().equals(Prefix.parse("4.4.4.4/32")))
            .findAny()
            .get();

    // policies should be generated only for the active ones
    assertThat(
        gr1.getGenerationPolicy(),
        equalTo(
            JuniperConfiguration.computeGeneratedRouteGenerationPolicyName(
                Prefix.parse("1.0.0.0/8"))));
    assertThat(
        gr2.getGenerationPolicy(),
        equalTo(
            JuniperConfiguration.computeGeneratedRouteGenerationPolicyName(
                Prefix.parse("2.0.0.0/8"))));
    assertThat(gr3.getGenerationPolicy(), nullValue());

    // the second one should only accept 2.0.0.0/32 as a contributor
    RoutingPolicy rp2 = config.getRoutingPolicies().get(gr2.getGenerationPolicy());
    ConnectedRoute cr31 = new ConnectedRoute(Prefix.parse("2.0.0.0/31"), "blah");
    ConnectedRoute cr32 = new ConnectedRoute(Prefix.parse("2.0.0.0/32"), "blah");
    assertThat(rp2.process(cr31, Bgpv4Route.testBuilder(), Direction.OUT), equalTo(false));
    assertThat(rp2.process(cr32, Bgpv4Route.testBuilder(), Direction.OUT), equalTo(true));

    // none should be discard routes
    assertThat(gr1.getDiscard(), equalTo(false));
    assertThat(gr2.getDiscard(), equalTo(false));
    assertThat(gr3.getDiscard(), equalTo(false));
    assertThat(gr4.getDiscard(), equalTo(false));
    assertThat(gr4, hasTag(3));
  }

  @Test
  public void testGeneratedRouteCommunities() throws IOException {
    Configuration config =
        BatfishTestUtils.parseTextConfigs(
                _folder, "org/batfish/grammar/juniper/testconfigs/generated-route-communities")
            .get("generated-route-communities");
    assertThat(
        getOnlyElement(config.getDefaultVrf().getGeneratedRoutes())
            .getCommunities()
            .getCommunities(),
        contains(StandardCommunity.of(65537L)));
  }

  /**
   * When local-address of a BGP peer group is unconfigured and default-address-selection flag is
   * on, BGP peer local IP is null and the VRF's source IP inference is set to use loopback IP.
   */
  @Test
  public void testBgpDefaultAddressSelection() throws IOException {
    Configuration config = parseConfig("bgp-default-address-selection");
    Ip loopback = Ip.parse("1.1.1.1");
    assertEquals(config.getDefaultVrf().getSourceIpInference(), UseConstantIp.create(loopback));
    assertThat(
        getOnlyElement(config.getDefaultVrf().getBgpProcess().getAllPeerConfigs()).getLocalIp(),
        nullValue());
  }

  @Test
  public void testNestedConfig() throws IOException {
    String hostname = "nested-config";

    /* Confirm a simple extraction (hostname) works for nested config format */
    assertThat(parseTextConfigs(hostname).keySet(), contains(hostname));
  }

  @Test
  public void testNestedConfigLineComments() throws IOException {
    String hostname = "nested-config-line-comments";

    // Confirm extraction works for nested configs even in the presence of line comments
    assertThat(parseTextConfigs(hostname).keySet(), contains(hostname));
  }

  @Test
  public void testNestedConfigWithMultilineComment() throws IOException {
    String hostname = "nested-config-with-multiline-comment";

    // Confirm extraction works for nested configs even in the presence of multiline comments
    assertThat(parseTextConfigs(hostname).keySet(), contains(hostname));
  }

  /**
   * Test that when a hierarchical config has a single missing quote, we can still extract later
   * data.
   */
  @Test
  public void testNestedConfigWithQuoteBug() throws IOException {
    String hostname = "nested-config-with-quote-bug";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    batfish.getSettings().setDisableUnrecognized(false);
    Map<String, Configuration> configs = batfish.loadConfigurations(batfish.getSnapshot());
    assertThat(configs, hasKeys(hostname));
    assertThat(configs.get(hostname).getVrfs(), hasKey("VRF_NAME"));
  }

  /** Test for https://github.com/batfish/batfish/issues/7225. */
  @Test
  public void testNestedConfigWithSecretData() {
    String hostname = "nested-config-with-secret-data";
    Configuration c = parseConfig(hostname);
    assertThat(c.getVrfs(), hasKey("VRF_NAME"));
    Vrf v = c.getVrfs().get("VRF_NAME");
    assertThat(v.getBgpProcess().getActiveNeighbors(), hasKey(Ip.parse("8.8.8.8")));
    BgpActivePeerConfig peer = v.getBgpProcess().getActiveNeighbors().get(Ip.parse("8.8.8.8"));
    assertThat(peer, allOf(hasLocalAs(1L), hasRemoteAs(60951L)));
  }

  @Test
  public void testNestedConfigStructureDef() throws IOException {
    String hostname = "nested-config-structure-def";
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    /* Confirm defined structures in nested config show up with original definition line numbers */
    assertThat(
        ccae,
        hasDefinedStructureWithDefinitionLines(
            filename, FIREWALL_FILTER, "FILTER1", contains(6, 7, 8, 9, 10, 11, 12, 13, 14, 15)));
    assertThat(
        ccae,
        hasDefinedStructureWithDefinitionLines(
            filename, FIREWALL_FILTER, "FILTER2", contains(16, 17, 18, 19, 20, 21, 22)));
  }

  @Test
  public void testOspfAreaDefaultMetric() throws IOException {
    Configuration config =
        BatfishTestUtils.parseTextConfigs(
                _folder, "org/batfish/grammar/juniper/testconfigs/ospf-area-default-metric")
            .get("ospf-area-default-metric");
    OspfArea area1 =
        config.getDefaultVrf().getOspfProcesses().get(DEFAULT_VRF_NAME).getAreas().get(1L);
    assertThat(area1, hasInjectDefaultRoute());
    assertThat(area1, hasMetricOfDefaultRoute(equalTo(10)));

    OspfArea area2 =
        config.getDefaultVrf().getOspfProcesses().get(DEFAULT_VRF_NAME).getAreas().get(2L);
    assertThat(area2, not(hasInjectDefaultRoute()));
  }

  /** Test that we allow integers as area Ids */
  @Test
  public void testOspfAreaInt() throws IOException {
    Configuration config =
        BatfishTestUtils.parseTextConfigs(
                _folder, "org/batfish/grammar/juniper/testconfigs/ospf-area-int")
            .get("ospf-area-int");
    assertTrue(
        config.getDefaultVrf().getOspfProcesses().get(DEFAULT_VRF_NAME).getAreas().containsKey(0L));
    assertTrue(
        config.getDefaultVrf().getOspfProcesses().get(DEFAULT_VRF_NAME).getAreas().containsKey(1L));
  }

  /** Test that we warn on uknown IPs as interfaces */
  @Test
  public void testOspfUnknownInterfaceIp() throws IOException {
    String hostname = "ospf-unknown-interface-ip";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ParseVendorConfigurationAnswerElement pvcae =
        batfish.loadParseVendorConfigurationAnswerElement(batfish.getSnapshot());
    assertThat(
        pvcae,
        hasParseWarning(
            "configs/" + hostname,
            containsString("Could not find interface with ip address: 1.1.1.1")));
  }

  @Test
  public void testOspfInterfaceDisable() {
    // Config has interfaces ge-0/0/1.0 and ge-0/0/2.0 configured in OSPF.
    // Interface ge-0/0/2.0 has disable set in OSPF config.
    Configuration config = parseConfig("ospf-interface-disable");
    assertThat(
        config.getVrfs().get(DEFAULT_VRF_NAME).getOspfProcesses(),
        hasEntry(equalTo(DEFAULT_VRF_NAME), notNullValue()));
    assertThat(config.getActiveInterfaces().get("ge-0/0/1.0"), hasOspfEnabled());
    assertThat(config.getActiveInterfaces().get("ge-0/0/2.0"), not(hasOspfEnabled()));
  }

  @Test
  public void testOspfDisable() {
    /*
    - Default VRF has OSPF process disabled, with interface ge-0/0/0.0
    - VRF INSTANCE_1 has OSPF process disabled, with interface ge-0/0/1.0
    - VRF INSTANCE_2 has OSPF process disabled, then enabled, with interface ge-0/0/2.0
    - VRF INSTANCE_3 has OSPF process enabled, then disabled, with interface ge-0/0/3.0
     */
    Configuration config = parseConfig("ospf-disable");

    // Default VRF: OSPF process should not be created
    assertThat(config.getVrfs().get(DEFAULT_VRF_NAME).getOspfProcesses(), anEmptyMap());
    assertThat(config.getActiveInterfaces().get("ge-0/0/0.0"), not(hasOspfEnabled()));

    // INSTANCE_1: OSPF process should not be created
    assertThat(config.getVrfs().get("INSTANCE_1").getOspfProcesses(), anEmptyMap());
    assertThat(config.getActiveInterfaces().get("ge-0/0/1.0"), not(hasOspfEnabled()));

    // INSTANCE_2: OSPF process should be created and its interface enabled
    assertThat(
        config.getVrfs().get("INSTANCE_2").getOspfProcesses(),
        hasEntry(equalTo("INSTANCE_2"), notNullValue()));
    assertThat(config.getActiveInterfaces().get("ge-0/0/2.0"), hasOspfEnabled());

    // INSTANCE_3: OSPF process should not be created
    assertThat(config.getVrfs().get("INSTANCE_3").getOspfProcesses(), anEmptyMap());
    assertThat(config.getActiveInterfaces().get("ge-0/0/3.0"), not(hasOspfEnabled()));
  }

  @Test
  public void testOspfMetric() throws IOException {
    Configuration config =
        BatfishTestUtils.parseTextConfigs(
                _folder, "org/batfish/grammar/juniper/testconfigs/ospfmetric")
            .get("ospfmetric");
    OspfAreaSummary summary =
        config
            .getDefaultVrf()
            .getOspfProcesses()
            .get(DEFAULT_VRF_NAME)
            .getAreas()
            .get(1L)
            .getSummaries()
            .get(Prefix.parse("10.0.0.0/16"));
    assertThat(summary, allOf(not(isAdvertised()), installsDiscard(), hasMetric(123L)));

    // Defaults
    summary =
        config
            .getDefaultVrf()
            .getOspfProcesses()
            .get(DEFAULT_VRF_NAME)
            .getAreas()
            .get(2L)
            .getSummaries()
            .get(Prefix.parse("10.0.0.0/16"));
    assertThat(summary, allOf(isAdvertised(), installsDiscard(), hasMetric(nullValue())));

    // Interface override
    assertThat(config, hasInterface("fe-1/0/1.0", hasOspfCost(equalTo(17))));
  }

  @Test
  public void testOspfPsk() {
    /* allow both encrypted and unencrypted key */
    parseConfig("ospf-psk");
  }

  @Test
  public void testOspfSummaryDiscardMetric() {
    String hostname = "ospf-reference-bandwidth";
    Configuration c = parseConfig(hostname);
    assertThat(
        c.getDefaultVrf().getOspfProcesses().get(DEFAULT_VRF_NAME).getSummaryDiscardMetric(),
        equalTo(OSPF_INTERNAL_SUMMARY_DISCARD_METRIC));
  }

  @Test
  public void testOspfReferenceBandwidth() {
    String hostname = "ospf-reference-bandwidth";
    Configuration c = parseConfig(hostname);
    assertThat(c, hasDefaultVrf(hasOspfProcess(DEFAULT_VRF_NAME, hasReferenceBandwidth(1E9D))));
    assertThat(c, hasVrf("vrf1", hasOspfProcess("vrf1", hasReferenceBandwidth(2E9D))));
    assertThat(c, hasVrf("vrf2", hasOspfProcess("vrf2", hasReferenceBandwidth(3E9D))));
    assertThat(c, hasVrf("vrf3", hasOspfProcess("vrf3", hasReferenceBandwidth(4E9D))));
    assertThat(c, hasVrf("vrf4", hasOspfProcess("vrf4", hasReferenceBandwidth(5E9D))));
  }

  @Test
  public void testPsPreferenceBehavior() {
    Configuration c = parseConfig("policy-statement-preference");

    RoutingPolicy policyPreference = c.getRoutingPolicies().get("preference");

    StaticRoute staticRoute =
        StaticRoute.testBuilder()
            .setNetwork(Prefix.parse("10.0.1.0/24"))
            .setNextHopInterface("nextint")
            .setNextHopIp(Ip.parse("10.0.0.1"))
            .setAdministrativeCost(1)
            .build();

    Environment.Builder eb = Environment.builder(c).setDirection(IN);
    policyPreference.call(
        eb.setOriginalRoute(staticRoute).setOutputRoute(OspfExternalType2Route.builder()).build());

    // Checking admin cost set on the output route
    assertThat(eb.build().getOutputRoute().getAdmin(), equalTo(123));
  }

  @Test
  public void testPsPreferenceStructure() {
    Configuration c = parseConfig("policy-statement-preference");

    RoutingPolicy policyPreference = c.getRoutingPolicies().get("preference");

    assertThat(policyPreference.getStatements(), hasSize(2));

    // Extracting the If statement
    assertThat(policyPreference.getStatements().get(0), instanceOf(If.class));

    If i = (If) policyPreference.getStatements().get(0);

    assertThat(i.getTrueStatements(), hasSize(1));
    assertThat(getOnlyElement(i.getTrueStatements()), instanceOf(TraceableStatement.class));
    TraceableStatement traceableStatement = (TraceableStatement) i.getTrueStatements().get(0);
    assertThat(
        getOnlyElement(traceableStatement.getInnerStatements()),
        instanceOf(SetAdministrativeCost.class));

    assertThat(
        getOnlyElement(traceableStatement.getInnerStatements()),
        isSetAdministrativeCostThat(hasAdmin(isLiteralIntThat(hasVal(123)))));
  }

  @Test
  public void testTacplusPsk() {
    /* allow both encrypted and unencrypted key */
    parseConfig("tacplus-psk");
  }

  @Test
  public void testIkePolicy() {
    Configuration c = parseConfig("ike-policy");

    assertThat(
        c,
        hasIkePhase1Policy(
            "policy1",
            allOf(
                hasIkePhase1Key(
                    IkePhase1KeyMatchers.hasKeyHash(
                        CommonUtil.sha256Digest("psk1" + CommonUtil.salt()))),
                hasIkePhase1Proposals(equalTo(ImmutableList.of("proposal1"))))));

    assertThat(
        c,
        hasIkePhase1Policy(
            "policy2",
            allOf(
                hasIkePhase1Key(
                    IkePhase1KeyMatchers.hasKeyHash(
                        CommonUtil.sha256Digest("psk1" + CommonUtil.salt()))),
                hasIkePhase1Proposals(equalTo(ImmutableList.of("proposal1"))))));
  }

  @Test
  public void testIkeProposal() {
    Configuration c = parseConfig("ike-proposal");

    // test for IKE phase1 proposals
    assertThat(
        c,
        hasIkePhase1Proposal(
            "proposal1",
            allOf(
                IkePhase1ProposalMatchers.hasEncryptionAlgorithm(EncryptionAlgorithm.THREEDES_CBC),
                IkePhase1ProposalMatchers.hasAuthenticationMethod(
                    IkeAuthenticationMethod.PRE_SHARED_KEYS),
                IkePhase1ProposalMatchers.hasHashingAlgorithm(IkeHashingAlgorithm.MD5),
                IkePhase1ProposalMatchers.hasDiffieHellmanGroup(DiffieHellmanGroup.GROUP14),
                IkePhase1ProposalMatchers.hasLifeTimeSeconds(50000))));
  }

  @Test
  public void testInterfaceArp() {
    Configuration c = parseConfig("interface-arp");

    /* The additional ARP IP set for irb.0 should appear in the data model */
    assertThat(c, hasInterface("irb.0", hasAdditionalArpIps(containsIp(Ip.parse("1.0.0.2")))));
  }

  @Test
  public void testInterfaceBandwidth() {
    Configuration c = parseConfig("interface-bandwidth");

    // Configuration has ge-0/0/0 with four units configured bandwidths 5000000000, 5000000k, 5000m,
    // 5g. Physical interface should have default bandwidth (1E9), unit interfaces should have 5E9.
    double unitBandwidth = 5E9;
    double physicalBandwidth =
        org.batfish.representation.juniper.Interface.getDefaultBandwidthByName("ge-0/0/0");

    assertThat(c, hasInterface("ge-0/0/0", hasBandwidth(physicalBandwidth)));
    assertThat(c, hasInterface("ge-0/0/0.0", hasBandwidth(unitBandwidth)));
    assertThat(c, hasInterface("ge-0/0/0.1", hasBandwidth(unitBandwidth)));
    assertThat(c, hasInterface("ge-0/0/0.2", hasBandwidth(unitBandwidth)));
    assertThat(c, hasInterface("ge-0/0/0.3", hasBandwidth(unitBandwidth)));

    // Configuration has ge-1/0/0 with one unit with configured bandwidth 10c (1c = 384 bps).
    // Physical interface should have default bandwidth (1E9), unit 3840.
    assertThat(c, hasInterface("ge-1/0/0", hasBandwidth(physicalBandwidth)));
    assertThat(c, hasInterface("ge-1/0/0.0", hasBandwidth(3840)));

    // Management interfaces have bandwidth 1 Gbps.
    assertThat(c, hasInterface("em0", hasBandwidth(1e9)));
  }

  @Test
  public void testInterfaceMtu() {
    Configuration c = parseConfig("interfaceMtu");

    /* Properly configured interfaces should be present in respective areas. */
    assertThat(c.getAllInterfaces().keySet(), hasItem("xe-0/0/0:0.0"));
    assertThat(c, hasInterface("xe-0/0/0:0.0", hasMtu(9000)));
  }

  @Test
  public void testInterfaceNativeVlan() {
    String hostname = "interface-native-vlan";
    Configuration c = parseConfig(hostname);

    assertThat(c, hasInterface("ge-0/1/0.0", hasNativeVlan(3)));
    assertThat(c, hasInterface("ge-0/2/0.0", hasNativeVlan(nullValue())));
    assertThat(c, hasInterface("ge-0/3/0.0", hasNativeVlan(nullValue())));
  }

  @Test
  public void testInterfaceOspfNetworkType() {
    String hostname = "ospf-interface-network-type";
    Configuration c = parseConfig(hostname);
    // Interface is assumed broadcast by default
    assertThat(
        c, hasInterface("ge-0/0/1.0", hasOspfNetworkType(equalTo(OspfNetworkType.BROADCAST))));
    // Confirm explicitly specified point-to-point interface shows up as such in the VI model
    assertThat(
        c, hasInterface("ge-0/0/0.0", hasOspfNetworkType(equalTo(OspfNetworkType.POINT_TO_POINT))));
    assertThat(
        c,
        hasInterface(
            "ge-0/0/2.0", hasOspfNetworkType(equalTo(OspfNetworkType.NON_BROADCAST_MULTI_ACCESS))));
    assertThat(
        c,
        hasInterface(
            "ge-0/0/3.0", hasOspfNetworkType(equalTo(OspfNetworkType.POINT_TO_MULTIPOINT))));
  }

  @Test
  public void testInterfacePrimary() throws IOException {
    String hostname = "interface-primary";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    JuniperConfiguration jc =
        (JuniperConfiguration)
            batfish.loadVendorConfigurations(batfish.getSnapshot()).get(hostname);
    ParseVendorConfigurationAnswerElement pvcae =
        batfish.loadParseVendorConfigurationAnswerElement(batfish.getSnapshot());

    Map<String, org.batfish.representation.juniper.Interface> units =
        jc.getMasterLogicalSystem().getInterfaces().get("em0").getUnits();

    assertTrue(units.get("em0.0").getPrimary());
    assertFalse(units.get("em0.1").getPrimary());

    // duplicate primary is ignored and we get a warning
    assertFalse(units.get("em0.2").getPrimary());
    assertThat(
        pvcae,
        hasParseWarning(
            "configs/" + hostname,
            containsString(
                "Cannot make em0.2 as the primary interface. em0.0 is already configured as"
                    + " primary.")));
  }

  @Test
  public void testInterfaceVniExtraction() {
    JuniperConfiguration c = parseJuniperConfig("interface-vni");
    Integer vni = c.getMasterLogicalSystem().getNamedVlans().get("VLAN_TEST").getVniId();
    Integer vni0 = 10101;
    assertEquals(vni, vni0);
  }

  @Test
  public void testIpAddressParsing() throws IOException {
    // Doesn't throw.
    parseConfig("ip_address");
  }

  @Test
  public void testJuniperOspfIntervals() {
    JuniperConfiguration config = parseJuniperConfig("ospf-intervals");
    Map<String, org.batfish.representation.juniper.Interface> ifaces =
        config.getMasterLogicalSystem().getInterfaces();

    String iface0 = "ge-0/0/0";
    String iface1 = "ge-0/0/1";
    String iface2 = "ge-0/0/2";
    String iface3 = "ge-0/0/3";
    assertThat(ifaces, hasKeys(iface0, iface1, iface2, iface3));

    OspfInterfaceSettings iface0unit0 =
        ifaces.get(iface0).getUnits().get(iface0 + ".0").getOspfSettings();
    OspfInterfaceSettings iface1unit0 =
        ifaces.get(iface1).getUnits().get(iface1 + ".0").getOspfSettings();
    OspfInterfaceSettings iface2unit0 =
        ifaces.get(iface2).getUnits().get(iface2 + ".0").getOspfSettings();
    OspfInterfaceSettings iface3unit0 =
        ifaces.get(iface3).getUnits().get(iface3 + ".0").getOspfSettings();

    // Confirm explicitly set hello and dead intervals show up in the VS model
    // Also confirm intervals that are not set show up as nulls in the VS model
    assertThat(iface0unit0.getOspfDeadInterval(), nullValue());
    assertThat(iface0unit0.getOspfHelloInterval(), equalTo(11));

    assertThat(iface1unit0.getOspfDeadInterval(), equalTo(22));
    assertThat(iface1unit0.getOspfHelloInterval(), equalTo(2));

    assertThat(iface2unit0.getOspfDeadInterval(), equalTo(44));
    assertThat(iface2unit0.getOspfHelloInterval(), nullValue());

    // not added to OSPF
    assertThat(iface3unit0, nullValue());
  }

  @Test
  public void testInterfaceRange() {
    String hostname = "interface-range";
    JuniperConfiguration juniperConfig = parseJuniperConfig(hostname);

    // range definitions are inserted properly into the vendor model
    InterfaceRange ae1 =
        juniperConfig.getMasterLogicalSystem().getInterfaceRanges().get("ae1-members");
    assertThat(ae1.getMtu(), equalTo(8000));
    assertThat(ae1.getDescription(), equalTo("dodo"));
    assertThat(
        ae1.getMembers(), equalTo(ImmutableList.of(new InterfaceRangeMember("xe-0/0/[0,1]"))));
    assertThat(
        ae1.getMemberRanges(),
        equalTo(ImmutableList.of(new InterfaceRangeMemberRange("xe-0/0/0", "xe-0/0/1"))));

    InterfaceRange ae2 =
        juniperConfig.getMasterLogicalSystem().getInterfaceRanges().get("ae2-members");
    assertThat(ae2.getDescription(), equalTo("dodo"));
    assertThat(ae2.getMembers(), equalTo(ImmutableList.of(new InterfaceRangeMember("xe-8/1/2"))));
    assertThat(ae2.get8023adInterface(), equalTo("ae1"));
    assertThat(ae2.getRedundantParentInterface(), equalTo("reth0"));

    // all interfaces should show up; no need to test their specific settings here
    Configuration c = parseConfig("interface-range");
    assertThat(c.getAllInterfaces(), hasKeys("xe-0/0/0", "xe-0/0/1", "xe-8/1/2", "reth0"));
  }

  @Test
  public void testInterfaceUndefined() {
    // Should not crash.
    parseConfig("interface-undefined");
  }

  @Test
  public void testInterfaceVlan() {
    Configuration c = parseConfig("interface-vlan");

    // Expecting an Interface in ACCESS mode with VLAN 101
    assertThat(c, hasInterface("ge-0/0/0.0", hasSwitchPortMode(SwitchportMode.ACCESS)));
    assertThat(c, hasInterface("ge-0/0/0.0", hasAccessVlan(101)));

    // Expecting an Interface in ACCESS mode with VLAN 103
    assertThat(c, hasInterface("ge-0/2/0.0", hasSwitchPortMode(SwitchportMode.ACCESS)));
    assertThat(c, hasInterface("ge-0/2/0.0", hasAccessVlan(103)));

    // Expecting an Interface in TRUNK mode with VLANs 1-5
    assertThat(c, hasInterface("ge-0/3/0.0", hasSwitchPortMode(SwitchportMode.TRUNK)));
    assertThat(
        c, hasInterface("ge-0/3/0.0", hasAllowedVlans(IntegerSpace.of(new SubRange("1-5")))));

    // Expecting interface with encapsulation VLAN set to .0:1000 .1:1
    assertThat(c, hasInterface("ge-0/4/0.0", hasEncapsulationVlan(1000)));
    assertThat(c, hasInterface("ge-0/4/0.1", hasEncapsulationVlan(1)));

    // Without vlan-tagging enabled, encapsulation vlan is ignored
    assertThat(c, hasInterface("ge-0/5/0.7", hasEncapsulationVlan(nullValue())));

    // Expecting an Interface in TRUNK mode with VLANs 6
    assertThat(c, hasInterface("ge-0/6/0.0", hasSwitchPortMode(SwitchportMode.TRUNK)));
    assertThat(c, hasInterface("ge-0/6/0.0", hasAllowedVlans(IntegerSpace.of(6))));
    assertThat(c, hasInterface("ge-0/6/0.1", hasEncapsulationVlan(100)));

    // Cannot configure trunk mode on unit 1
    assertThat(c, hasInterface("ge-0/7/0.1", hasSwitchPortMode(SwitchportMode.NONE)));
    assertThat(c, hasInterface("ge-0/7/0.1", hasAllowedVlans(IntegerSpace.EMPTY)));
    assertThat(c, hasInterface("ge-0/7/0.1", hasAccessVlan(nullValue())));

    // Expecting an Interface in ACCESS mode with VLAN 200
    assertThat(c, hasInterface("ge-0/8/0.0", hasSwitchPortMode(SwitchportMode.ACCESS)));
    assertThat(c, hasInterface("ge-0/8/0.0", hasAccessVlan(200)));

    // Expecting an Interface in TRUNK mode with allowed VLANs 200, 300-400, and 500
    assertThat(c, hasInterface("ge-0/9/0.0", hasSwitchPortMode(SwitchportMode.TRUNK)));
    assertThat(
        c,
        hasInterface(
            "ge-0/9/0.0",
            hasAllowedVlans(
                IntegerSpace.builder()
                    .including(200)
                    .including(new SubRange("300-400"))
                    .including(500)
                    .build())));

    // Vlan "default" resets the vlan config
    assertThat(
        c,
        hasInterface(
            "ge-0/10/0.0", allOf(hasAccessVlan(nullValue()), hasAllowedVlans(IntegerSpace.EMPTY))));
  }

  @Test
  public void testDhcpRelayServerGroup() throws IOException {
    JuniperConfiguration c = parseJuniperConfig("dhcp-relay-server-group");

    SortedMap<String, DhcpRelayServerGroup> serverGroups =
        c.getMasterLogicalSystem().getRoutingInstances().get("RI").getDhcpRelayServerGroups();

    assertTrue(serverGroups.get("EMPTY").getServers().isEmpty());
    assertThat(serverGroups.get("SG1").getServers(), contains(Ip.parse("1.1.1.1")));
  }

  @Test
  public void testInterfaceVlanReferences() throws IOException {
    String hostname = "interface-vlan";
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    /*
     * We expect an undefined reference for VLAN_TEST_UNDEFINED
     */
    assertThat(ccae, hasUndefinedReference(filename, VLAN, "VLAN_TEST_UNDEFINED", INTERFACE_VLAN));

    /*
     * Named VLANs
     */
    assertThat(
        ccae.getDefinedStructures()
            .get(filename)
            .getOrDefault(VLAN.getDescription(), Collections.emptySortedMap()),
        hasKeys(
            "VLAN_ID_TEST",
            "VLAN_ID_LIST_TEST_SINGLETON",
            "VLAN_ID_LIST_TEST_RANGE",
            "VLAN_WITH_INTERFACES",
            "VLAN_TEST_UNUSED"));
  }

  @Test
  public void testInterfaceVtnet() {
    Configuration c = parseConfig("interface-vtnet");

    // Virtualized physical interface.
    assertThat(c, hasInterface("vtnet0", hasInterfaceType(InterfaceType.PHYSICAL)));
    assertThat(
        c,
        hasInterface(
            "vtnet0.0", hasAllAddresses(contains(ConcreteInterfaceAddress.parse("10.1.2.1/30")))));
  }

  @Test
  public void testIrbInterfaces() {
    String hostname = "irb-interfaces";
    Configuration c = parseConfig(hostname);

    // No parent 'irb' interface should be created
    assertThat(c.getAllInterfaces(), not(hasKey("irb")));

    // irb.0 should be created
    assertThat(c.getAllInterfaces(), hasKey("irb.0"));

    Interface irb0 = c.getAllInterfaces().get("irb.0");

    // irb.0 should not have bind dependency to "irb", since it is not a real parent interface
    assertThat(
        irb0.getDependencies(),
        not(hasItem(equalTo(new Interface.Dependency("irb", DependencyType.BIND)))));

    // verify interface type
    assertThat(irb0.getInterfaceType(), equalTo(InterfaceType.VLAN));

    // verify vlan assignment
    assertThat(irb0.getVlan(), equalTo(5));
  }

  @Test
  public void testFirewallFilterDscp() {
    String hostname = "firewall-filter-dscp";
    Configuration c = parseConfig(hostname);

    Flow.Builder flowBuilder = Flow.builder().setIngressNode(c.getHostname());

    // Test custom alias
    assertThat(
        c, hasIpAccessList("FILTER1", accepts(flowBuilder.setDscp(0b001000).build(), null, c)));
    assertThat(
        c, hasIpAccessList("FILTER1", rejects(flowBuilder.setDscp(0b111111).build(), null, c)));

    // Test builtin alias
    assertThat(
        c,
        hasIpAccessList(
            "FILTER2",
            accepts(flowBuilder.setDscp(DscpUtil.defaultValue("cs1").get()).build(), null, c)));
    assertThat(
        c, hasIpAccessList("FILTER2", rejects(flowBuilder.setDscp(0b111111).build(), null, c)));

    // Test constant value
    assertThat(c, hasIpAccessList("FILTER3", accepts(flowBuilder.setDscp(3).build(), null, c)));
    assertThat(
        c, hasIpAccessList("FILTER3", rejects(flowBuilder.setDscp(0b111111).build(), null, c)));
  }

  @Test
  public void testIpProtocol() {
    String hostname = "firewall-filter-ip-protocol";
    Configuration c = parseConfig(hostname);

    Flow tcpFlow = createFlow(IpProtocol.TCP, 0);
    Flow icmpFlow =
        builder()
            .setIngressNode("node")
            .setIpProtocol(IpProtocol.ICMP)
            .setIcmpType(0)
            .setIcmpCode(0)
            .build();

    // Tcp flow should be accepted by the filter and others should be rejected
    assertThat(c, hasIpAccessList("FILTER", accepts(tcpFlow, null, c)));
    assertThat(c, hasIpAccessList("FILTER", rejects(icmpFlow, null, c)));
  }

  @Test
  public void testSourceAddress() {
    Configuration c = parseConfig("firewall-source-address");
    String filename = "configs/" + c.getHostname();
    String filterNameV4 = "FILTER";
    String filterNameV6 = "FILTERv6";

    assertThat(c.getIpAccessLists().keySet(), containsInAnyOrder(filterNameV4, filterNameV6));

    IpAccessList fwSourceAddressAcl = c.getIpAccessLists().get(filterNameV4);
    assertThat(fwSourceAddressAcl.getLines(), hasSize(2));

    // should have the same acl as defined in the config
    assertThat(
        c,
        hasIpAccessList(
            filterNameV4,
            IpAccessListMatchers.hasLines(
                equalTo(
                    ExprAclLine.builder()
                        .setAction(LineAction.PERMIT)
                        .setMatchCondition(
                            or(
                                matchSrc(
                                    IpWildcard.ipWithWildcardMask(
                                            Ip.parse("1.0.3.0"), Ip.parse("0.255.0.255"))
                                        .toIpSpace(),
                                    TraceElement.of("Matched source-address 1.2.3.4/255.0.255.0")),
                                matchSrc(
                                    IpWildcard.parse("2.3.4.5/24").toIpSpace(),
                                    TraceElement.of("Matched source-address 2.3.4.5/24"))))
                        .setName("TERM")
                        .setTraceElement(matchingFirewallFilterTerm(filename, filterNameV4, "TERM"))
                        .setVendorStructureId(
                            firewallFilterTermVendorStructureId(filename, filterNameV4, "TERM"))
                        .build()),
                equalTo(
                    ExprAclLine.builder()
                        .setAction(LineAction.PERMIT)
                        .setMatchCondition(
                            new AndMatchExpr(
                                ImmutableList.of(
                                    matchSrc(
                                        IpWildcard.parse("0.0.0.0/0").toIpSpace(),
                                        TraceElement.of("Matched source-address 0.0.0.0/0")),
                                    and(
                                        new MatchHeaderSpace(
                                            HeaderSpace.builder()
                                                .setNotSrcIps(
                                                    IpWildcard.parse("1.1.1.1/32").toIpSpace())
                                                .build(),
                                            TraceElement.of(
                                                "Matched source-address 1.1.1.1/32 except")),
                                        new MatchHeaderSpace(
                                            HeaderSpace.builder()
                                                .setNotSrcIps(
                                                    IpWildcard.parse("2.2.2.2/32").toIpSpace())
                                                .build(),
                                            TraceElement.of(
                                                "Matched source-address 2.2.2.2/32 except"))))))
                        .setName("TERM-EXCEPT")
                        .setTraceElement(
                            matchingFirewallFilterTerm(filename, filterNameV4, "TERM-EXCEPT"))
                        .setVendorStructureId(
                            firewallFilterTermVendorStructureId(
                                filename, filterNameV4, "TERM-EXCEPT"))
                        .build()))));
  }

  @Test
  public void testIpsecBugs() {
    // don't crash
    parseConfig("ipsec-bugs");
  }

  @Test
  public void testIpsecPolicy() {
    Configuration c = parseConfig("ipsec-policy");

    // tests for conversion to IPSec phase 2 policies
    assertThat(
        c,
        hasIpsecPhase2Policy(
            "policy1",
            allOf(
                IpsecPhase2PolicyMatchers.hasIpsecProposals(
                    equalTo(ImmutableList.of("TRANSFORM-SET1", "TRANSFORM-SET2"))),
                IpsecPhase2PolicyMatchers.hasPfsKeyGroup(equalTo(DiffieHellmanGroup.GROUP14)))));

    assertThat(
        c,
        hasIpsecPhase2Policy(
            "policy2",
            allOf(
                IpsecPhase2PolicyMatchers.hasIpsecProposals(equalTo(ImmutableList.of())),
                IpsecPhase2PolicyMatchers.hasPfsKeyGroup(equalTo(DiffieHellmanGroup.GROUP15)))));

    assertThat(
        c,
        hasIpsecPhase2Policy(
            "policy3",
            allOf(
                IpsecPhase2PolicyMatchers.hasIpsecProposals(equalTo(ImmutableList.of())),
                IpsecPhase2PolicyMatchers.hasPfsKeyGroup(equalTo(DiffieHellmanGroup.GROUP16)))));

    assertThat(
        c,
        hasIpsecPhase2Policy(
            "policy4",
            allOf(
                IpsecPhase2PolicyMatchers.hasIpsecProposals(equalTo(ImmutableList.of())),
                IpsecPhase2PolicyMatchers.hasPfsKeyGroup(equalTo(DiffieHellmanGroup.GROUP19)))));

    assertThat(
        c,
        hasIpsecPhase2Policy(
            "policy5",
            allOf(
                IpsecPhase2PolicyMatchers.hasIpsecProposals(equalTo(ImmutableList.of())),
                IpsecPhase2PolicyMatchers.hasPfsKeyGroup(equalTo(DiffieHellmanGroup.GROUP20)))));
    assertThat(
        c,
        hasIpsecPhase2Policy(
            "policy6",
            allOf(
                IpsecPhase2PolicyMatchers.hasIpsecProposals(equalTo(ImmutableList.of())),
                IpsecPhase2PolicyMatchers.hasPfsKeyGroup(equalTo(DiffieHellmanGroup.GROUP5)))));

    assertThat(
        c,
        hasIpsecPhase2Policy(
            "policy7",
            allOf(
                IpsecPhase2PolicyMatchers.hasIpsecProposals(
                    equalTo(ImmutableList.of("TRANSFORM-SET1"))),
                IpsecPhase2PolicyMatchers.hasPfsKeyGroup(equalTo(DiffieHellmanGroup.GROUP14)))));
  }

  @Test
  public void testIpsecProposalSet() {
    Configuration c = parseConfig("ipsec-proposal-set");

    assertThat(
        c,
        hasIpsecPhase2Proposal(
            "NOPFS_ESP_3DES_MD5",
            allOf(
                IpsecPhase2ProposalMatchers.hasAuthenticationAlgorithm(
                    IpsecAuthenticationAlgorithm.HMAC_MD5_96),
                IpsecPhase2ProposalMatchers.hasEncryptionAlgorithm(
                    EncryptionAlgorithm.THREEDES_CBC),
                IpsecPhase2ProposalMatchers.hasProtocols(ImmutableSortedSet.of(IpsecProtocol.ESP)),
                IpsecPhase2ProposalMatchers.hasIpsecEncapsulationMode(
                    IpsecEncapsulationMode.TUNNEL))));

    assertThat(
        c,
        hasIpsecPhase2Proposal(
            "NOPFS_ESP_3DES_SHA",
            allOf(
                IpsecPhase2ProposalMatchers.hasAuthenticationAlgorithm(
                    IpsecAuthenticationAlgorithm.HMAC_SHA1_96),
                IpsecPhase2ProposalMatchers.hasEncryptionAlgorithm(
                    EncryptionAlgorithm.THREEDES_CBC),
                IpsecPhase2ProposalMatchers.hasProtocols(ImmutableSortedSet.of(IpsecProtocol.ESP)),
                IpsecPhase2ProposalMatchers.hasIpsecEncapsulationMode(
                    IpsecEncapsulationMode.TUNNEL))));

    assertThat(
        c,
        hasIpsecPhase2Proposal(
            "NOPFS_ESP_DES_MD5",
            allOf(
                IpsecPhase2ProposalMatchers.hasAuthenticationAlgorithm(
                    IpsecAuthenticationAlgorithm.HMAC_MD5_96),
                IpsecPhase2ProposalMatchers.hasEncryptionAlgorithm(EncryptionAlgorithm.DES_CBC),
                IpsecPhase2ProposalMatchers.hasProtocols(ImmutableSortedSet.of(IpsecProtocol.ESP)),
                IpsecPhase2ProposalMatchers.hasIpsecEncapsulationMode(
                    IpsecEncapsulationMode.TUNNEL))));

    assertThat(
        c,
        hasIpsecPhase2Proposal(
            "NOPFS_ESP_DES_SHA",
            allOf(
                IpsecPhase2ProposalMatchers.hasAuthenticationAlgorithm(
                    IpsecAuthenticationAlgorithm.HMAC_SHA1_96),
                IpsecPhase2ProposalMatchers.hasEncryptionAlgorithm(EncryptionAlgorithm.DES_CBC),
                IpsecPhase2ProposalMatchers.hasProtocols(ImmutableSortedSet.of(IpsecProtocol.ESP)),
                IpsecPhase2ProposalMatchers.hasIpsecEncapsulationMode(
                    IpsecEncapsulationMode.TUNNEL))));

    assertThat(
        c,
        hasIpsecPhase2Proposal(
            "G2_ESP_3DES_SHA",
            allOf(
                IpsecPhase2ProposalMatchers.hasAuthenticationAlgorithm(
                    IpsecAuthenticationAlgorithm.HMAC_SHA1_96),
                IpsecPhase2ProposalMatchers.hasEncryptionAlgorithm(
                    EncryptionAlgorithm.THREEDES_CBC),
                IpsecPhase2ProposalMatchers.hasProtocols(ImmutableSortedSet.of(IpsecProtocol.ESP)),
                IpsecPhase2ProposalMatchers.hasIpsecEncapsulationMode(
                    IpsecEncapsulationMode.TUNNEL))));

    assertThat(
        c,
        hasIpsecPhase2Proposal(
            "G2_ESP_AES128_SHA",
            allOf(
                IpsecPhase2ProposalMatchers.hasAuthenticationAlgorithm(
                    IpsecAuthenticationAlgorithm.HMAC_SHA1_96),
                IpsecPhase2ProposalMatchers.hasEncryptionAlgorithm(EncryptionAlgorithm.AES_128_CBC),
                IpsecPhase2ProposalMatchers.hasProtocols(ImmutableSortedSet.of(IpsecProtocol.ESP)),
                IpsecPhase2ProposalMatchers.hasIpsecEncapsulationMode(
                    IpsecEncapsulationMode.TUNNEL))));

    assertThat(
        c,
        hasIpsecPhase2Policy(
            "policy1",
            allOf(
                IpsecPhase2PolicyMatchers.hasIpsecProposals(
                    equalTo(ImmutableList.of("G2_ESP_3DES_SHA", "G2_ESP_AES128_SHA"))),
                IpsecPhase2PolicyMatchers.hasPfsKeyGroup(equalTo(DiffieHellmanGroup.GROUP2)))));

    assertThat(
        c,
        hasIpsecPhase2Policy(
            "policy2",
            allOf(
                IpsecPhase2PolicyMatchers.hasIpsecProposals(
                    equalTo(
                        ImmutableList.of(
                            "NOPFS_ESP_3DES_SHA",
                            "NOPFS_ESP_3DES_MD5",
                            "NOPFS_ESP_DES_SHA",
                            "NOPFS_ESP_DES_MD5"))),
                IpsecPhase2PolicyMatchers.hasPfsKeyGroups(empty()))));
  }

  @Test
  public void testIpsecProposalToIpsecPhase2Proposal() {
    Configuration c = parseConfig("ipsec-proposal");
    assertThat(
        c,
        hasIpsecPhase2Proposal(
            "prop1",
            allOf(
                IpsecPhase2ProposalMatchers.hasAuthenticationAlgorithm(
                    IpsecAuthenticationAlgorithm.HMAC_MD5_96),
                IpsecPhase2ProposalMatchers.hasEncryptionAlgorithm(EncryptionAlgorithm.AES_128_CBC),
                IpsecPhase2ProposalMatchers.hasProtocols(
                    ImmutableSortedSet.of(IpsecProtocol.ESP, IpsecProtocol.AH)),
                IpsecPhase2ProposalMatchers.hasIpsecEncapsulationMode(
                    IpsecEncapsulationMode.TUNNEL))));
    assertThat(
        c,
        hasIpsecPhase2Proposal(
            "prop2",
            allOf(
                IpsecPhase2ProposalMatchers.hasAuthenticationAlgorithm(
                    IpsecAuthenticationAlgorithm.HMAC_SHA1_96),
                IpsecPhase2ProposalMatchers.hasEncryptionAlgorithm(EncryptionAlgorithm.AES_192_CBC),
                IpsecPhase2ProposalMatchers.hasProtocols(ImmutableSortedSet.of(IpsecProtocol.AH)),
                IpsecPhase2ProposalMatchers.hasIpsecEncapsulationMode(
                    IpsecEncapsulationMode.TUNNEL))));
    assertThat(
        c,
        hasIpsecPhase2Proposal(
            "prop3",
            allOf(
                IpsecPhase2ProposalMatchers.hasAuthenticationAlgorithm(
                    IpsecAuthenticationAlgorithm.HMAC_MD5_96),
                IpsecPhase2ProposalMatchers.hasEncryptionAlgorithm(
                    EncryptionAlgorithm.THREEDES_CBC),
                IpsecPhase2ProposalMatchers.hasProtocols(ImmutableSortedSet.of(IpsecProtocol.ESP)),
                IpsecPhase2ProposalMatchers.hasIpsecEncapsulationMode(
                    IpsecEncapsulationMode.TUNNEL))));
    assertThat(
        c,
        hasIpsecPhase2Proposal(
            "prop4",
            allOf(
                IpsecPhase2ProposalMatchers.hasAuthenticationAlgorithm(
                    IpsecAuthenticationAlgorithm.HMAC_MD5_96),
                IpsecPhase2ProposalMatchers.hasEncryptionAlgorithm(EncryptionAlgorithm.AES_128_GCM),
                IpsecPhase2ProposalMatchers.hasProtocols(ImmutableSortedSet.of(IpsecProtocol.ESP)),
                IpsecPhase2ProposalMatchers.hasIpsecEncapsulationMode(
                    IpsecEncapsulationMode.TUNNEL))));
    assertThat(
        c,
        hasIpsecPhase2Proposal(
            "prop5",
            allOf(
                IpsecPhase2ProposalMatchers.hasAuthenticationAlgorithm(
                    IpsecAuthenticationAlgorithm.HMAC_MD5_96),
                IpsecPhase2ProposalMatchers.hasEncryptionAlgorithm(EncryptionAlgorithm.AES_192_GCM),
                IpsecPhase2ProposalMatchers.hasProtocols(ImmutableSortedSet.of(IpsecProtocol.ESP)),
                IpsecPhase2ProposalMatchers.hasIpsecEncapsulationMode(
                    IpsecEncapsulationMode.TUNNEL))));
    assertThat(
        c,
        hasIpsecPhase2Proposal(
            "prop6",
            allOf(
                IpsecPhase2ProposalMatchers.hasAuthenticationAlgorithm(
                    IpsecAuthenticationAlgorithm.HMAC_MD5_96),
                IpsecPhase2ProposalMatchers.hasEncryptionAlgorithm(EncryptionAlgorithm.AES_256_GCM),
                IpsecPhase2ProposalMatchers.hasProtocols(ImmutableSortedSet.of(IpsecProtocol.ESP)),
                IpsecPhase2ProposalMatchers.hasIpsecEncapsulationMode(
                    IpsecEncapsulationMode.TUNNEL))));
  }

  @Test
  public void testToIpsecPeerConfig() {
    Configuration c = parseConfig("ipsec-vpn");

    assertThat(
        c,
        hasIpsecPeerConfig(
            "ike-vpn-chicago",
            isIpsecStaticPeerConfigThat(
                allOf(
                    hasDestinationAddress(Ip.parse("198.51.100.102")),
                    IpsecPeerConfigMatchers.hasIkePhase1Policy("ike-phase1-policy"),
                    IpsecPeerConfigMatchers.hasIpsecPolicy("ipsec-phase2-policy"),
                    IpsecPeerConfigMatchers.hasSourceInterface("ge-0/0/3.0"),
                    IpsecPeerConfigMatchers.hasLocalAddress(Ip.parse("198.51.100.2")),
                    IpsecPeerConfigMatchers.hasTunnelInterface(equalTo("st0.0"))))));
  }

  @Test
  public void testAddress() {
    Configuration c = parseConfig("firewall-address");

    assertThat(c.getIpAccessLists(), hasKeys("FILTER"));
    IpAccessList filter = c.getIpAccessLists().get("FILTER");
    Ip allowed1 = Ip.parse("1.0.0.0");
    Ip allowed2 = Ip.parse("2.0.0.0");
    Ip rejected = Ip.parse("3.3.3.3");

    // Both addresses permitted, same or different rules
    assertThat(filter, accepts(createFlow(allowed1, allowed1), null, c));
    assertThat(filter, accepts(createFlow(allowed2, allowed2), null, c));
    assertThat(filter, accepts(createFlow(allowed1, allowed2), null, c));
    // Allowed if EITHER is allowed.
    assertThat(filter, accepts(createFlow(allowed1, rejected), null, c));
    assertThat(filter, accepts(createFlow(rejected, allowed1), null, c));
    assertThat(filter, accepts(createFlow(rejected, allowed2), null, c));
    // Rejected if BOTH rejected
    assertThat(filter, rejects(createFlow(rejected, rejected), null, c));
  }

  @Test
  public void testDestinationAddress() {
    Configuration c = parseConfig("firewall-destination-address");
    String filename = "configs/" + c.getHostname();
    String filterNameV4 = "FILTER";
    String filterNameV6 = "FILTERv6";

    assertThat(c.getIpAccessLists().keySet(), containsInAnyOrder(filterNameV4, filterNameV6));

    IpAccessList fwDestinationAddressAcl = c.getIpAccessLists().get(filterNameV4);
    assertThat(fwDestinationAddressAcl.getLines(), hasSize(1));

    // should have the same acl as defined in the config
    assertThat(
        c,
        hasIpAccessList(
            filterNameV4,
            IpAccessListMatchers.hasLines(
                equalTo(
                    ImmutableList.of(
                        ExprAclLine.builder()
                            .setAction(LineAction.PERMIT)
                            .setMatchCondition(
                                or(
                                    matchDst(
                                        IpWildcard.ipWithWildcardMask(
                                                Ip.parse("1.0.3.0"), Ip.parse("0.255.0.255"))
                                            .toIpSpace(),
                                        TraceElement.of(
                                            "Matched destination-address"
                                                + " 1.2.3.4/255.0.255.0")),
                                    matchDst(
                                        IpWildcard.parse("2.3.4.5/24").toIpSpace(),
                                        TraceElement.of("Matched destination-address 2.3.4.5/24"))))
                            .setName("TERM")
                            .setTraceElement(
                                matchingFirewallFilterTerm(filename, filterNameV4, "TERM"))
                            .setVendorStructureId(
                                firewallFilterTermVendorStructureId(filename, filterNameV4, "TERM"))
                            .build())))));
  }

  @Test
  public void testSourceAddressBehavior() {
    Configuration c = parseConfig("firewall-source-address");

    assertThat(c.getIpAccessLists().keySet(), hasSize(2));

    Flow whiteListedSrc = createFlow("1.8.3.9", "2.5.6.7");
    Flow blackListedSrc = createFlow("2.2.2.2", "2.5.6.7");

    IpAccessList incomingFilter = c.getAllInterfaces().get("xe-0/0/0.0").getIncomingFilter();

    // Whitelisted source address should be allowed
    assertThat(incomingFilter, accepts(whiteListedSrc, "xe-0/0/0.0", c));

    // Blacklisted source address should be denied
    assertThat(incomingFilter, rejects(blackListedSrc, "xe-0/0/0.0", c));
  }

  @Test
  public void testDestinationAddressBehavior() {
    Configuration c = parseConfig("firewall-destination-address");

    assertThat(c.getIpAccessLists().keySet(), hasSize(2));

    Flow whiteListedDst = createFlow("2.5.6.7", "1.8.3.9");
    Flow blackListedDst = createFlow("2.5.6.7", "5.8.4.9");

    IpAccessList incomingFilter = c.getAllInterfaces().get("xe-0/0/0.0").getIncomingFilter();

    // Whitelisted source address should be allowed
    assertThat(incomingFilter, accepts(whiteListedDst, "xe-0/0/0.0", c));

    // Blacklisted source address should be denied
    assertThat(incomingFilter, rejects(blackListedDst, "xe-0/0/0.0", c));
  }

  @Test
  public void testJuniperApplyGroupsChain() {
    Configuration c = parseConfig("apply-groups-chain");
    assertThat(
        c,
        hasInterface(
            "em0.0", hasAllAddresses(contains(ConcreteInterfaceAddress.parse("1.1.1.1/31")))));
  }

  @Test
  public void testJuniperApplyGroupsNode() throws IOException {
    String filename = "juniper-apply-groups-node";

    Batfish batfish = getBatfishForConfigurationNames(filename);
    Configuration c =
        batfish.loadConfigurations(batfish.getSnapshot()).entrySet().iterator().next().getValue();

    /* hostname should not be overwritten from node0 nor node1 group */
    assertThat(c, hasHostname(filename));
    /* other lines from node0 and node1 groups should be applied */
    assertThat(
        c,
        hasInterface(
            "lo0.1", hasAllAddresses(contains(ConcreteInterfaceAddress.parse("1.1.1.1/32")))));
    assertThat(
        c,
        hasInterface(
            "lo0.2", hasAllAddresses(contains(ConcreteInterfaceAddress.parse("2.2.2.2/32")))));
  }

  @Test
  public void testJuniperApplyGroupsNodeNoHostname() throws IOException {
    String filename = "juniper-apply-groups-node-no-hostname";

    Batfish batfish = getBatfishForConfigurationNames(filename);
    Configuration c =
        batfish.loadConfigurations(batfish.getSnapshot()).entrySet().iterator().next().getValue();

    /* hostname should be generated, and not gotten from node0 nor node1 group */
    assertThat(c, hasHostname(not(equalTo("juniper-apply-groups-node0"))));
    assertThat(c, hasHostname(not(equalTo("juniper-apply-groups-node1"))));
    /* other lines from node0 and node1 groups should be applied */
    assertThat(
        c,
        hasInterface(
            "lo0.1", hasAllAddresses(contains(ConcreteInterfaceAddress.parse("1.1.1.1/32")))));
    assertThat(
        c,
        hasInterface(
            "lo0.2", hasAllAddresses(contains(ConcreteInterfaceAddress.parse("2.2.2.2/32")))));
  }

  @Test
  public void testJuniperIsis() {
    String hostname = "juniper-isis";
    Configuration c = parseConfig(hostname);

    double expectedReferenceBandwidth = 100E9;
    assertThat(c, hasDefaultVrf(hasIsisProcess(IsisProcessMatchers.hasLevel1(nullValue()))));
    assertThat(
        c, hasDefaultVrf(hasIsisProcess(IsisProcessMatchers.hasLevel2(hasWideMetricsOnly()))));
    assertThat(c, hasDefaultVrf(hasIsisProcess(hasOverload(false))));
    assertThat(
        c,
        hasDefaultVrf(
            hasIsisProcess(IsisProcessMatchers.hasReferenceBandwidth(expectedReferenceBandwidth))));

    {
      Interface loopback = c.getActiveInterfaces().get("lo0.0");
      assertThat(loopback, hasIsis(hasIsoAddress(new IsoAddress("12.1234.1234.1234.1234.00"))));
      assertThat(loopback, hasIsis(hasLevel1(nullValue())));
      // Loopbacks are always passive for IS-IS regardless of configuration
      assertThat(loopback, hasIsis(hasLevel2(hasMode(IsisInterfaceMode.PASSIVE))));

      // Loopback did not set an IS-IS metric, so its cost should be based on the reference
      // bandwidth.
      // First confirm the expected cost isn't coincidentally equal to the Juniper default cost of
      // 10.
      // No need to worry about getBandwidth() returning null for Juniper interfaces.
      long expectedCost =
          Math.max((long) (expectedReferenceBandwidth / loopback.getBandwidth()), 1L);
      assertThat(expectedCost, not(equalTo(10L)));
      assertThat(loopback, hasIsis(hasLevel2(hasCost(expectedCost))));
    }

    {
      Interface physical = c.getActiveInterfaces().get("ge-0/0/0.0");
      assertThat(physical, hasIsis(hasIsoAddress(new IsoAddress("12.1234.1234.1234.1234.01"))));
      assertThat(physical, hasIsis(hasBfdLivenessDetectionMinimumInterval(250)));
      assertThat(physical, hasIsis(hasBfdLivenessDetectionMultiplier(3)));
      assertThat(physical, hasIsis(IsisInterfaceSettingsMatchers.hasPointToPoint()));
      assertThat(physical, hasIsis(hasLevel1(nullValue())));
      assertThat(physical, hasIsis(hasLevel2(hasCost(5L))));
      // Explicitly configured passive
      assertThat(physical, hasIsis(hasLevel2(hasMode(IsisInterfaceMode.ACTIVE))));
      assertThat(
          physical,
          hasIsis(hasLevel2(hasHelloAuthenticationType(IsisHelloAuthenticationType.MD5))));
      assertThat(
          physical, hasIsis(hasLevel2(IsisInterfaceLevelSettingsMatchers.hasHelloInterval(1))));
      assertThat(physical, hasIsis(hasLevel2(hasHoldTime(3))));
    }

    {
      // Assert non-ISIS interface has no ISIS, but has IP address
      Interface nonIsis = c.getActiveInterfaces().get("ge-1/0/0.0");
      assertThat(nonIsis, hasIsis(nullValue()));
      assertThat(
          nonIsis,
          hasAllAddresses(contains(ConcreteInterfaceAddress.create(Ip.parse("10.1.1.1"), 24))));
    }
  }

  @Test
  public void testIsisRedistribution() throws IOException {
    /*
    Setup: r1 and r2 share an IS-IS edge.

    r1 has a static route 1.2.3.4/30, but no IS-IS export policy. r2 should not have that route.

    r2 has static routes 2.2.2.0/30 and 5.6.7.8/30. Its IS-IS export policy exports static routes
    with destinations in 5.0.0.0/8. r1 should have an IS-IS route for 5.6.7.8/30 but not 2.2.2.0/30.
     */
    String testrigName = "isis-redist";
    String r1 = "r1";
    String r2 = "r2";
    List<String> configurationNames = ImmutableList.of(r1, r2);
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationFiles(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    batfish.computeDataPlane(batfish.getSnapshot());
    DataPlane dp = batfish.loadDataPlane(batfish.getSnapshot());
    Set<AbstractRoute> r1Routes = dp.getRibs().get(r1, DEFAULT_VRF_NAME).getRoutes();
    Set<AbstractRoute> r2Routes = dp.getRibs().get(r2, DEFAULT_VRF_NAME).getRoutes();

    // 1.2.3.4/30 does not get exported to r2 because r1 has no IS-IS export policy.
    assertThat(r1Routes, hasItem(hasPrefix(Prefix.parse("1.2.3.4/30"))));
    assertThat(r2Routes, not(hasItem(hasPrefix(Prefix.parse("1.2.3.4/30")))));

    // 2.2.2.0/30 does not get exported to r1 because it doesn't match r2's IS-IS export policy.
    assertThat(r1Routes, not(hasItem(hasPrefix(Prefix.parse("2.2.2.0/30")))));
    assertThat(r2Routes, hasItem(hasPrefix(Prefix.parse("2.2.2.0/30"))));

    // 5.6.7.8/30 does get exported to r1. Should be an external L1 route with default IS-IS metric.
    RoutingProtocol protocol = RoutingProtocol.ISIS_EL1;
    int adminCost = protocol.getDefaultAdministrativeCost(ConfigurationFormat.JUNIPER);
    assertThat(
        r1Routes,
        hasItem(
            allOf(
                hasPrefix(Prefix.parse("5.6.7.8/30")),
                hasProtocol(protocol),
                hasAdministrativeCost(adminCost),
                AbstractRouteDecoratorMatchers.hasMetric(
                    Integer.toUnsignedLong(DEFAULT_ISIS_COST)))));
  }

  @Test
  public void testIsisMinimal() {
    // This config has an IS-IS loopback and IS-IS interface.
    // Should contain an IS-IS process even though no level is explicitly configured.
    Configuration c = parseConfig("isis-minimal");
    assertThat(c, hasDefaultVrf(hasIsisProcess(notNullValue())));
  }

  @Test
  public void testIsisL1Disabled() {
    // This config has a loopback with an ISO address and "set protocols isis interface lo0.0" and
    // an interface ge-0/0/1.0 with both levels enabled.
    // Then in [protocols isis level 1] it sets "disable" and "wide-metrics-only".
    // Setting wide-metrics-only should not re-enable level 1.
    // None of the level 1 configuration should affect level 2 (which is enabled by default).
    Configuration c = parseConfig("isis-disabled-l1");
    IsisProcess proc = c.getVrfs().get(DEFAULT_VRF_NAME).getIsisProcess();
    assertThat(proc.getLevel1(), nullValue());
    assertThat(proc.getLevel2(), notNullValue());
  }

  @Test
  public void testIsisIgnoreAttachedBit() {
    parseConfig("isis-ignore-attached-bit");
    // don't crash.
  }

  @Test
  public void testIsisInterfaceAndLevelDisable() {
    Configuration c = parseConfig("isis-interface-and-level-disable");
    IsisProcess proc = c.getVrfs().get(DEFAULT_VRF_NAME).getIsisProcess();
    assertThat(proc.getLevel1(), notNullValue());
    assertThat(proc.getLevel2(), notNullValue());

    // Interfaces ge-0/0/0.0, ge-0/0/1.0, and ge-0/0/2.0 all have ISO addresses
    // ge-0/0/0.0 is disabled for IS-IS: set protocols isis interface ge-0/0/0.0 disable
    // ge-0/0/1.0 has level 1 disabled: set protocols isis interface ge-0/0/1.0 level 1 disable
    // ge-0/0/2.0 doesn't have anything disabled
    IsisInterfaceSettings iface0Settings = c.getActiveInterfaces().get("ge-0/0/0.0").getIsis();
    IsisInterfaceSettings iface1Settings = c.getActiveInterfaces().get("ge-0/0/1.0").getIsis();
    IsisInterfaceSettings iface2Settings = c.getActiveInterfaces().get("ge-0/0/2.0").getIsis();
    assertNull(iface0Settings);
    assertThat(iface1Settings.getLevel1(), nullValue());
    assertThat(iface1Settings.getLevel2(), notNullValue());
    assertThat(iface2Settings.getLevel1(), notNullValue());
    assertThat(iface2Settings.getLevel2(), notNullValue());
  }

  @Test
  public void testJuniperIsisNoIsoAddress() {
    Configuration c = parseConfig("juniper-isis-no-iso");

    assertThat(c, hasDefaultVrf(hasIsisProcess(nullValue())));
  }

  @Test
  public void testJuniperIsisNonLoopbackIsoAddress() {
    Configuration c = parseConfig("juniper-isis-iso-non-loopback");

    assertThat(
        c,
        hasDefaultVrf(
            hasIsisProcess(hasNetAddress(equalTo(new IsoAddress("12.1234.1234.1234.1234.01"))))));
  }

  @Test
  public void testJuniperIsisNoReferenceBandwidth() {
    Configuration c = parseConfig("juniper-isis-no-reference-bandwidth");

    // With no set metric or reference bandwidth, Juniper IS-IS cost should default to 10
    assertThat(
        c, hasDefaultVrf(hasIsisProcess(IsisProcessMatchers.hasReferenceBandwidth((Double) null))));
    assertThat(c, hasInterface("lo0.0", hasIsis(hasLevel2(hasCost(10L)))));
  }

  @Test
  public void testJuniperIsisOverload() {
    Configuration c = parseConfig("juniper-isis-overload");
    assertThat(c, hasDefaultVrf(hasIsisProcess(IsisProcessMatchers.hasOverload(true))));
  }

  @Test
  public void testJuniperIsisOverloadWithTimeout() {
    Configuration c = parseConfig("juniper-isis-overload-with-timeout");
    assertThat(c, hasDefaultVrf(hasIsisProcess(IsisProcessMatchers.hasOverload(false))));
  }

  @Test
  public void testJuniperIsisPassive() {
    Configuration c = parseConfig("juniper-isis-passive");
    Interface passiveL1 = c.getActiveInterfaces().get("ge-1/2/0.0");
    Interface passiveIface = c.getActiveInterfaces().get("ge-1/2/0.1");
    // set protocols isis interface ge-1/2/0.0 level 1 passive
    assertThat(
        passiveL1,
        hasIsis(
            allOf(
                hasLevel1(hasMode(IsisInterfaceMode.PASSIVE)),
                hasLevel2(hasMode(IsisInterfaceMode.ACTIVE)))));
    // set protocols isis interface ge-1/2/0.1 passive
    assertThat(
        passiveIface,
        hasIsis(
            allOf(
                hasLevel1(hasMode(IsisInterfaceMode.PASSIVE)),
                hasLevel2(hasMode(IsisInterfaceMode.PASSIVE)))));
  }

  @Test
  public void testJuniperOspfStubSettings() {
    Configuration c = parseConfig("juniper-ospf-stub-settings");

    // Get OSPF process
    assertThat(c, hasDefaultVrf(hasOspfProcess(DEFAULT_VRF_NAME, notNullValue())));
    OspfProcess proc = c.getVrfs().get(DEFAULT_VRF_NAME).getOspfProcesses().get(DEFAULT_VRF_NAME);

    // Check correct stub types are assigned
    assertThat(proc, hasArea(0L, hasStubType(StubType.NONE)));
    assertThat(proc, hasArea(1L, hasStubType(StubType.NSSA)));
    assertThat(proc, hasArea(2L, hasStubType(StubType.NSSA)));
    assertThat(proc, hasArea(3L, hasStubType(StubType.STUB)));
    assertThat(proc, hasArea(4L, hasStubType(StubType.STUB)));
    assertThat(proc, hasArea(5L, hasStubType(StubType.NONE)));
    assertThat(proc, hasArea(6L, hasStubType(StubType.STUB)));

    // Check for stub subtype settings
    assertThat(
        proc, hasArea(1L, hasNssa(hasDefaultOriginateType(OspfDefaultOriginateType.INTER_AREA))));
    assertThat(proc, hasArea(1L, hasNssa(hasSuppressType3(false))));
    assertThat(proc, hasArea(2L, hasNssa(hasDefaultOriginateType(OspfDefaultOriginateType.NONE))));
    assertThat(proc, hasArea(2L, hasNssa(hasSuppressType3())));
    assertThat(proc, hasArea(3L, hasStub(StubSettingsMatchers.hasSuppressType3(false))));
    assertThat(proc, hasArea(4L, hasStub(StubSettingsMatchers.hasSuppressType3())));
    assertThat(proc, hasArea(6L, hasStub(StubSettingsMatchers.hasSuppressType3())));
  }

  @Test
  public void testJuniperPolicyStatementPrefixListDisjunction() {
    // Configuration has policy statement with term that checks two prefix lists.
    Configuration c = parseConfig("juniper-from-prefix-list");

    // Accept if network matches either prefix list
    for (Prefix p : ImmutableList.of(Prefix.parse("1.1.1.0/24"), Prefix.parse("2.2.2.0/24"))) {
      Result result =
          c.getRoutingPolicies()
              .get("POLICY-NAME")
              .call(
                  Environment.builder(c)
                      .setOriginalRoute(new ConnectedRoute(p, "nextHop"))
                      .build());
      assertThat(result.getBooleanValue(), equalTo(true));
    }

    // Reject if network does not match protocol
    Result result =
        c.getRoutingPolicies()
            .get("POLICY-NAME")
            .call(
                Environment.builder(c)
                    .setOriginalRoute(
                        org.batfish.datamodel.StaticRoute.testBuilder()
                            .setAdministrativeCost(0)
                            .setNetwork(Prefix.parse("1.1.1.0/24"))
                            .build())
                    .build());
    assertThat(result.getBooleanValue(), equalTo(false));

    // Reject if network is missing from both prefix lists
    result =
        c.getRoutingPolicies()
            .get("POLICY-NAME")
            .call(
                Environment.builder(c)
                    .setOriginalRoute(new ConnectedRoute(Prefix.parse("3.3.3.0/24"), "nextHop"))
                    .build());
    assertThat(result.getBooleanValue(), equalTo(false));
  }

  @Test
  public void testJuniperPolicyStatement() throws IOException {
    String hostname = "juniper-policy-statement";
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    assertThat(ccae, hasNumReferrers(filename, POLICY_STATEMENT, "POLICY_NAME", 0));
    assertThat(
        ccae,
        hasNumReferrers(
            filename,
            POLICY_STATEMENT_TERM,
            computePolicyStatementTermName("POLICY_NAME", "TERM_NAME"),
            1));
  }

  @Test
  public void testJuniperPolicyStatementTermFromExtraction() {
    JuniperConfiguration c = parseJuniperConfig("juniper-policy-statement-from");
    {
      PolicyStatement policy = c.getMasterLogicalSystem().getPolicyStatements().get("COLOR_POLICY");
      assertThat(policy.getTerms(), hasKeys("TMIN", "TMAX"));
      assertThat(
          policy.getTerms().get("TMIN").getFroms().getFromColor(), equalTo(new PsFromColor(0)));
      assertThat(
          policy.getTerms().get("TMAX").getFroms().getFromColor(),
          equalTo(new PsFromColor(0xFFFFFFFFL)));
    }
    {
      PolicyStatement policy =
          c.getMasterLogicalSystem().getPolicyStatements().get("LOCAL_PREFERENCE_POLICY");
      assertThat(policy.getTerms(), hasKeys("TMIN", "TMAX"));
      assertThat(
          policy.getTerms().get("TMIN").getFroms().getFromLocalPreference(),
          equalTo(new PsFromLocalPreference(0)));
      assertThat(
          policy.getTerms().get("TMAX").getFroms().getFromLocalPreference(),
          equalTo(new PsFromLocalPreference(MAX_LOCAL_PREFERENCE)));
    }
    {
      PolicyStatement policy = c.getMasterLogicalSystem().getPolicyStatements().get("TAG_POLICY");
      assertThat(policy.getTerms(), hasKeys("TMIN", "TMAX"));
      assertThat(
          policy.getTerms().get("TMIN").getFroms().getFromTags(), contains(new PsFromTag(0)));
      assertThat(
          policy.getTerms().get("TMAX").getFroms().getFromTags(),
          contains(new PsFromTag(MAX_LOCAL_PREFERENCE)));
    }
  }

  @Test
  public void testJuniperPolicyStatementThenCommunity() {
    JuniperConfiguration c = parseJuniperConfig("juniper-ps-then-community");
    Map<String, PsTerm> pses =
        c.getMasterLogicalSystem().getPolicyStatements().get("PS").getTerms();
    assertThat(
        pses.get("MULTI_SET").getThens().getAllThens(), contains(new PsThenCommunitySet("COMM2")));
    assertThat(
        pses.get("SET_ADD").getThens().getAllThens(),
        contains(new PsThenCommunitySet("COMM1"), new PsThenCommunityAdd("COMM2")));
    assertThat(
        pses.get("ADD_SET").getThens().getAllThens(), contains(new PsThenCommunitySet("COMM2")));
  }

  @Test
  public void testJuniperPolicyStatementTermThenExtraction() {
    JuniperConfiguration c = parseJuniperConfig("juniper-policy-statement-then");
    {
      PolicyStatement policy = c.getMasterLogicalSystem().getPolicyStatements().get("COLOR_POLICY");
      assertThat(
          policy.getTerms(),
          hasKeys("TSETMIN", "TADDMAX", "TSUB3", "T2SETMIN", "T2ADDMAX", "T2SUB3"));
      // TODO: implement then color, then color2
    }
    {
      PolicyStatement policy =
          c.getMasterLogicalSystem().getPolicyStatements().get("LOCAL_PREFERENCE_POLICY");
      assertThat(policy.getTerms(), hasKeys("TSETMIN", "TADDMAX", "TSUB3"));
      assertThat(
          policy.getTerms().get("TSETMIN").getThens().getAllThens(),
          contains(new PsThenLocalPreference(0, Operator.SET)));
      assertThat(
          policy.getTerms().get("TADDMAX").getThens().getAllThens(),
          contains(new PsThenLocalPreference(MAX_LOCAL_PREFERENCE, Operator.ADD)));
      assertThat(
          policy.getTerms().get("TSUB3").getThens().getAllThens(),
          contains(new PsThenLocalPreference(3, Operator.SUBTRACT)));
    }
    {
      PolicyStatement policy = c.getMasterLogicalSystem().getPolicyStatements().get("TAG_POLICY");
      assertThat(policy.getTerms(), hasKeys("TMIN", "TMAX"));
      assertThat(
          policy.getTerms().get("TMIN").getThens().getAllThens(), contains(new PsThenTag(0)));
      assertThat(
          policy.getTerms().get("TMAX").getThens().getAllThens(), contains(new PsThenTag(MAX_TAG)));
    }
    {
      PolicyStatement policy =
          c.getMasterLogicalSystem().getPolicyStatements().get("TUNNEL_ATTR_POLICY");
      assertThat(policy.getTerms(), hasKeys("SET_TUNNEL_ATTR", "REMOVE_TUNNEL_ATTR"));
      assertThat(
          policy.getTerms().get("SET_TUNNEL_ATTR").getThens().getAllThens(),
          contains(new PsThenTunnelAttributeSet("TA")));
      assertThat(
          policy.getTerms().get("REMOVE_TUNNEL_ATTR").getThens().getAllThens(),
          contains(PsThenTunnelAttributeRemove.INSTANCE));
    }
  }

  @Test
  public void testInterfaceFilter() {
    parseConfig("juniper-set-interface-filter-crash");
    // don't crash.
  }

  @Test
  public void testInterfaceDamping() {
    parseConfig("juniper-set-interface-damping");
    // don't crash.
  }

  @Test
  public void testBgpFamilyRouteTarget() {
    parseConfig("juniper-bgp-family-route-target");
    // don't crash.
  }

  @Test
  public void testJuniperPolicyStatementTermFromEvaluation() {
    // Configuration has policy statements
    Configuration c = parseConfig("juniper-policy-statement-term");
    Prefix testPrefix = Prefix.parse("1.1.1.1/28");
    Result result;

    /*
    COMMUNITY_POLICY should accept routes with either set community, but no others
    BGP1 matches 1, BGP2 matches 2
      set policy-options policy-statement COMMUNITY_POLICY term T1 from community BGP1
      set policy-options policy-statement COMMUNITY_POLICY term T1 from community BGP2
    */
    RoutingPolicy communityPolicy = c.getRoutingPolicies().get("COMMUNITY_POLICY");
    Bgpv4Route.Builder brb =
        Bgpv4Route.testBuilder()
            .setAdmin(100)
            .setNetwork(testPrefix)
            .setOriginatorIp(Ip.parse("2.2.2.2"))
            .setOriginType(OriginType.INCOMPLETE)
            .setProtocol(RoutingProtocol.BGP);
    result =
        communityPolicy.call(
            envWithRoute(c, brb.setCommunities(ImmutableSet.of(StandardCommunity.of(1L))).build()));
    assertThat(result.getBooleanValue(), equalTo(true));
    result =
        communityPolicy.call(
            envWithRoute(c, brb.setCommunities(ImmutableSet.of(StandardCommunity.of(2L))).build()));
    assertThat(result.getBooleanValue(), equalTo(true));
    result =
        communityPolicy.call(
            envWithRoute(c, brb.setCommunities(ImmutableSet.of(StandardCommunity.of(3L))).build()));
    assertThat(result.getBooleanValue(), equalTo(false));

    /* COMMUNITY_COUNT_POLICY should accept routes with 2 or fewer communities. */
    RoutingPolicy communityCountPolicy = c.getRoutingPolicies().get("COMMUNITY_COUNT_POLICY");
    result =
        communityCountPolicy.call(
            envWithRoute(c, brb.setCommunities(ImmutableSet.of(StandardCommunity.of(1L))).build()));
    assertThat(result.getBooleanValue(), equalTo(true));
    result =
        communityCountPolicy.call(
            envWithRoute(
                c,
                brb.setCommunities(
                        ImmutableSet.of(StandardCommunity.of(1L), StandardCommunity.of(2L)))
                    .build()));
    assertThat(result.getBooleanValue(), equalTo(true));
    result =
        communityCountPolicy.call(
            envWithRoute(
                c,
                brb.setCommunities(
                        ImmutableSet.of(
                            StandardCommunity.of(1L),
                            StandardCommunity.of(2L),
                            StandardCommunity.of(3L)))
                    .build()));
    assertThat(result.getBooleanValue(), equalTo(false));

    /*
    FAMILY_POLICY should accept only inet6 (each set overwrites previous)
      set policy-options policy-statement FAMILY_POLICY term T1 from family inet
      set policy-options policy-statement FAMILY_POLICY term T1 from family inet6
    */
    RoutingPolicy familyPolicy = c.getRoutingPolicies().get("FAMILY_POLICY");
    result = familyPolicy.call(envWithRoute(c, new ConnectedRoute(testPrefix, "nextHop")));
    assertThat(result.getBooleanValue(), equalTo(false));

    /*
    INTERFACE_POLICY should accept routes from either set interface, but not from other networks
      set interfaces ge-0/0/1 unit 0 family inet address 10.0.0.1/30
      set interfaces ge-0/0/2 unit 0 family inet address 10.0.0.5/30
      set policy-options policy-statement INTERFACE_POLICY term T1 from interface ge-0/0/1.0
      set policy-options policy-statement INTERFACE_POLICY term T1 from interface ge-0/0/2.0
    */
    RoutingPolicy interfacePolicy = c.getRoutingPolicies().get("INTERFACE_POLICY");
    result =
        interfacePolicy.call(
            envWithRoute(c, new ConnectedRoute(Prefix.parse("10.0.0.1/30"), "nextHop")));
    assertThat(result.getBooleanValue(), equalTo(true));
    result =
        interfacePolicy.call(
            envWithRoute(c, new ConnectedRoute(Prefix.parse("10.0.0.5/30"), "nextHop")));
    assertThat(result.getBooleanValue(), equalTo(true));
    result =
        interfacePolicy.call(
            envWithRoute(c, new ConnectedRoute(Prefix.parse("10.0.0.9/30"), "nextHop")));
    assertThat(result.getBooleanValue(), equalTo(false));

    {
      /*
       * Local-pref policy should accept route with local-pref 2, but not other values.
       * Last "from local-preference" statement overrides previous statements
       */
      RoutingPolicy localPrefPolicy = c.getRoutingPolicies().get("LOCAL_PREFERENCE_POLICY");
      Builder bgpRouteBuilder =
          Bgpv4Route.testBuilder()
              .setOriginatorIp(Ip.ZERO)
              .setNetwork(Prefix.ZERO)
              .setProtocol(RoutingProtocol.BGP)
              .setOriginType(OriginType.INCOMPLETE);
      result = localPrefPolicy.call(envWithRoute(c, bgpRouteBuilder.setLocalPreference(2).build()));
      assertTrue(result.getBooleanValue());

      result = localPrefPolicy.call(envWithRoute(c, bgpRouteBuilder.setLocalPreference(1).build()));
      assertFalse(result.getBooleanValue());

      result = localPrefPolicy.call(envWithRoute(c, bgpRouteBuilder.setLocalPreference(3).build()));
      assertFalse(result.getBooleanValue());
    }

    /*
    Metric policy should accept route with metric 100, but not other metrics.
    Last "from metric" statement overrides previous statements
      set policy-options policy-statement METRIC_POLICY term T1 from metric 50
      set policy-options policy-statement METRIC_POLICY term T1 from metric 100
     */
    RoutingPolicy metricPolicy = c.getRoutingPolicies().get("METRIC_POLICY");
    Builder bgpRouteBuilder =
        Bgpv4Route.testBuilder()
            .setOriginatorIp(Ip.ZERO)
            .setNetwork(Prefix.ZERO)
            .setProtocol(RoutingProtocol.BGP)
            .setOriginType(OriginType.INCOMPLETE);
    result = metricPolicy.call(envWithRoute(c, bgpRouteBuilder.setMetric(100).build()));
    assertTrue(result.getBooleanValue());

    result = metricPolicy.call(envWithRoute(c, bgpRouteBuilder.setMetric(50).build()));
    assertFalse(result.getBooleanValue());

    result = metricPolicy.call(envWithRoute(c, bgpRouteBuilder.setMetric(51).build()));
    assertFalse(result.getBooleanValue());

    /*
    Next-hop policy
     */
    RoutingPolicy nhPolicy = c.getRoutingPolicies().get("NEXT_HOP_POLICY");
    result =
        nhPolicy.call(
            envWithRoute(c, bgpRouteBuilder.setNextHop(NextHopDiscard.instance()).build(), IN));
    assertFalse(result.getBooleanValue());
    result =
        nhPolicy.call(
            envWithRoute(
                c, bgpRouteBuilder.setNextHop(NextHopIp.of(Ip.parse("1.2.3.4"))).build(), IN));
    assertTrue(result.getBooleanValue());
    result =
        nhPolicy.call(
            envWithRoute(
                c, bgpRouteBuilder.setNextHop(NextHopIp.of(Ip.parse("1.2.3.5"))).build(), IN));
    assertFalse(result.getBooleanValue());

    /*
    NETWORK_POLICY should accept routes with networks matching any prefix list or route filter line.
    Prefix lists are all defined as PLX = [ X.X.X.0/24 ].
      set policy-options policy-statement NETWORK_POLICY term T1 from prefix-list PL1
      set policy-options policy-statement NETWORK_POLICY term T1 from prefix-list PL2
      set policy-options policy-statement NETWORK_POLICY term T1 from prefix-list-filter PL3 longer
      set policy-options policy-statement NETWORK_POLICY term T1 from prefix-list-filter PL4 longer
      set policy-options policy-statement NETWORK_POLICY term T1 from prefix-list-filter PL5 orlonger
      set policy-options policy-statement NETWORK_POLICY term T1 from prefix-list-filter PL6 orlonger
      set policy-options policy-statement NETWORK_POLICY term T1 from route-filter 7.7.7.0/24 exact
      set policy-options policy-statement NETWORK_POLICY term T1 from route-filter 8.8.8.0/24 exact
    */
    RoutingPolicy networkPolicy = c.getRoutingPolicies().get("NETWORK_POLICY");
    StaticRoute.Builder srb = StaticRoute.testBuilder().setAdministrativeCost(100);
    // prefix-list statements should match exact length
    result =
        networkPolicy.call(envWithRoute(c, srb.setNetwork(Prefix.parse("1.1.1.0/24")).build()));
    assertThat(result.getBooleanValue(), equalTo(true));
    result =
        networkPolicy.call(envWithRoute(c, srb.setNetwork(Prefix.parse("2.2.2.0/24")).build()));
    assertThat(result.getBooleanValue(), equalTo(true));
    result =
        networkPolicy.call(envWithRoute(c, srb.setNetwork(Prefix.parse("1.1.1.0/25")).build()));
    assertThat(result.getBooleanValue(), equalTo(false));
    // prefix-list-filter longer statements should match any longer length, but not exact length
    result =
        networkPolicy.call(envWithRoute(c, srb.setNetwork(Prefix.parse("3.3.3.0/25")).build()));
    assertThat(result.getBooleanValue(), equalTo(true));
    result =
        networkPolicy.call(envWithRoute(c, srb.setNetwork(Prefix.parse("4.4.4.0/26")).build()));
    assertThat(result.getBooleanValue(), equalTo(true));
    result =
        networkPolicy.call(envWithRoute(c, srb.setNetwork(Prefix.parse("3.3.3.0/24")).build()));
    assertThat(result.getBooleanValue(), equalTo(false));
    // prefix-list-filter orlonger statements should match /24 or longer
    result =
        networkPolicy.call(envWithRoute(c, srb.setNetwork(Prefix.parse("5.5.5.0/24")).build()));
    assertThat(result.getBooleanValue(), equalTo(true));
    result =
        networkPolicy.call(envWithRoute(c, srb.setNetwork(Prefix.parse("5.5.5.0/25")).build()));
    assertThat(result.getBooleanValue(), equalTo(true));
    result =
        networkPolicy.call(envWithRoute(c, srb.setNetwork(Prefix.parse("6.6.6.0/26")).build()));
    assertThat(result.getBooleanValue(), equalTo(true));
    // route-filter statements should match exact length
    result =
        networkPolicy.call(envWithRoute(c, srb.setNetwork(Prefix.parse("7.7.7.0/24")).build()));
    assertThat(result.getBooleanValue(), equalTo(true));
    result =
        networkPolicy.call(envWithRoute(c, srb.setNetwork(Prefix.parse("8.8.8.0/24")).build()));
    assertThat(result.getBooleanValue(), equalTo(true));
    result =
        networkPolicy.call(envWithRoute(c, srb.setNetwork(Prefix.parse("7.7.7.0/25")).build()));
    assertThat(result.getBooleanValue(), equalTo(false));
    // shorter prefix should not match for any
    result =
        networkPolicy.call(envWithRoute(c, srb.setNetwork(Prefix.parse("1.1.1.0/23")).build()));
    assertThat(result.getBooleanValue(), equalTo(false));
    result =
        networkPolicy.call(envWithRoute(c, srb.setNetwork(Prefix.parse("3.3.3.0/23")).build()));
    assertThat(result.getBooleanValue(), equalTo(false));
    result =
        networkPolicy.call(envWithRoute(c, srb.setNetwork(Prefix.parse("5.5.5.0/23")).build()));
    assertThat(result.getBooleanValue(), equalTo(false));
    result =
        networkPolicy.call(envWithRoute(c, srb.setNetwork(Prefix.parse("7.7.7.0/23")).build()));
    assertThat(result.getBooleanValue(), equalTo(false));
    // exact /32 for ip address should match
    result =
        networkPolicy.call(envWithRoute(c, srb.setNetwork(Prefix.parse("9.9.9.9/32")).build()));
    assertThat(result.getBooleanValue(), equalTo(true));
    // random prefix should not match
    result =
        networkPolicy.call(envWithRoute(c, srb.setNetwork(Prefix.parse("99.99.99.0/24")).build()));
    assertThat(result.getBooleanValue(), equalTo(false));

    /*
    PROTOCOL_POLICY should accept routes with either set protocol, but no others
      set policy-options policy-statement PROTOCOL_POLICY term TERM1 from protocol direct
      set policy-options policy-statement PROTOCOL_POLICY term TERM1 from protocol static
    */
    RoutingPolicy protocolPolicy = c.getRoutingPolicies().get("PROTOCOL_POLICY");
    result = protocolPolicy.call(envWithRoute(c, new ConnectedRoute(testPrefix, "nextHop")));
    assertThat(result.getBooleanValue(), equalTo(true));
    result = protocolPolicy.call(envWithRoute(c, srb.build()));
    assertThat(result.getBooleanValue(), equalTo(true));
    result =
        protocolPolicy.call(
            envWithRoute(
                c,
                new LocalRoute(
                    ConcreteInterfaceAddress.create(Ip.parse("1.1.1.1"), 28), "nextHop")));
    assertThat(result.getBooleanValue(), equalTo(false));

    {
      /*
      ROUTE_TYPE should accept routes with IBGP protocol
        set policy-options policy-statement ROUTE_TYPE term T1 from protocol bgp
        set policy-options policy-statement ROUTE_TYPE term T1 from route-type internal
      */
      RoutingPolicy policy = c.getRoutingPolicies().get("ROUTE_TYPE");
      // Permit IBGP
      assertTrue(
          policy
              .call(envWithRoute(c, brb.setProtocol(RoutingProtocol.IBGP).build()))
              .getBooleanValue());
      // Reject EBGP
      assertFalse(
          policy
              .call(envWithRoute(c, brb.setProtocol(RoutingProtocol.BGP).build()))
              .getBooleanValue());
      // Reject static
      assertFalse(policy.call(envWithRoute(c, srb.build())).getBooleanValue());
    }

    /*
    TAG_POLICY should accept routes with either set tag, but not from other tags
      set policy-options policy-statement TAG_POLICY term T1 from tag 1
      set policy-options policy-statement TAG_POLICY term T1 from tag 2
    */
    RoutingPolicy tagPolicy = c.getRoutingPolicies().get("TAG_POLICY");
    srb = StaticRoute.testBuilder().setAdministrativeCost(100).setNetwork(testPrefix);
    result = tagPolicy.call(Environment.builder(c).setOutputRoute(srb.setTag(1L)).build());
    assertThat(result.getBooleanValue(), equalTo(true));
    result = tagPolicy.call(Environment.builder(c).setOutputRoute(srb.setTag(2L)).build());
    assertThat(result.getBooleanValue(), equalTo(true));
    result = tagPolicy.call(Environment.builder(c).setOutputRoute(srb.setTag(3L)).build());
    assertThat(result.getBooleanValue(), equalTo(false));

    /*
    AS_PATH_GROUP_POLICY should accept routes from as-path within as-path-group
    set policy-options policy-statement AS_PATH_GROUP_POLICY term T1 from as-path-group AS_PATH_GROUP
    */
    RoutingPolicy asPathGroupPolicy = c.getRoutingPolicies().get("AS_PATH_GROUP_POLICY");
    Bgpv4Route.Builder test =
        Bgpv4Route.testBuilder()
            .setAdmin(100)
            .setNetwork(testPrefix)
            .setOriginatorIp(Ip.parse("2.2.2.2"))
            .setOriginType(OriginType.INCOMPLETE)
            .setProtocol(RoutingProtocol.BGP);
    result =
        asPathGroupPolicy.call(
            envWithRoute(c, test.setAsPath(AsPath.ofSingletonAsSets(1L)).build()));
    assertThat(result.getBooleanValue(), equalTo(true));

    result =
        asPathGroupPolicy.call(
            envWithRoute(c, test.setAsPath(AsPath.ofSingletonAsSets(2L)).build()));
    assertThat(result.getBooleanValue(), equalTo(true));

    result =
        asPathGroupPolicy.call(
            envWithRoute(c, test.setAsPath(AsPath.ofSingletonAsSets(3L)).build()));
    assertThat(result.getBooleanValue(), equalTo(false));
  }

  @Test
  public void testJuniperPolicyStatementFromMetric2Parsing() {
    // Test that "from metric2" statements parse and are marked as unsupported
    String hostname = "juniper-policy-statement-from-metric2";
    JuniperConfiguration vc = parseJuniperConfig(hostname);

    // Verify that the configuration generates expected unsupported warnings
    List<ParseWarning> parseWarnings = vc.getWarnings().getParseWarnings();
    assertThat(
        parseWarnings,
        contains(
            allOf(hasComment("This feature is not currently supported"), hasText("metric2 0")),
            allOf(hasComment("This feature is not currently supported"), hasText("metric2 100"))));

    // Verify that the METRIC2_POLICY was parsed into the policy statement map
    assertThat(vc.getMasterLogicalSystem().getPolicyStatements(), hasKey("METRIC2_POLICY"));
    PolicyStatement metric2Policy =
        vc.getMasterLogicalSystem().getPolicyStatements().get("METRIC2_POLICY");
    assertThat(metric2Policy.getTerms(), hasKey("T1"));
  }

  private static Environment envWithRoute(Configuration c, AbstractRoute route) {
    return envWithRoute(c, route, Direction.OUT);
  }

  private static Environment envWithRoute(
      Configuration c, AbstractRoute route, Direction direction) {
    return Environment.builder(c)
        .setDirection(direction)
        .setOriginalRoute(route)
        .setOutputRoute(route.toBuilder())
        .build();
  }

  @Test
  public void testJuniperWildcards() {
    String hostname = "juniper-wildcards";
    String loopback = "lo0.0";
    String prefix1 = "1.1.1.1/32";
    String prefix2 = "3.3.3.3/32";
    String prefix3 = "88.1.2.3/32";
    String prefixList1 = "p1";
    String prefixList2 = "p2";
    String prefixList3 = "p3";
    Ip neighborIp = Ip.parse("2.2.2.2");

    Configuration c = parseConfig(hostname);

    /* apply-groups using group containing interface wildcard should function as expected. */
    assertThat(
        c,
        hasInterface(loopback, hasAllAddresses(contains(ConcreteInterfaceAddress.parse(prefix1)))));

    /* The wildcard copied out of groups should disappear and not be treated as an actual interface */
    assertThat(c, hasInterfaces(not(hasKey("*.*"))));

    /* The wildcard-looking interface description should not be pruned since its parse-tree node was not created via preprocessor. */
    assertThat(c, hasInterface(loopback, hasDescription("<SCRUBBED>")));

    /* apply-path should work with wildcard. Its line should not be pruned since its parse-tree node was not created via preprocessor. */
    assertThat(c, hasRouteFilterList(prefixList1, permits(Prefix.parse(prefix1))));

    /* prefix-list p2 should get content from g2, but no prefix-list named "<*>" should be created */
    assertThat(c, hasRouteFilterList(prefixList2, permits(Prefix.parse(prefix2))));
    assertThat(c, hasRouteFilterLists(not(hasKey("<*>"))));

    /* prefix-list p3 should get only address from ge-0/0/0.0*/
    assertThat(c, hasRouteFilterList(prefixList3, permits(Prefix.parse(prefix3))));
    assertThat(
        c, hasRouteFilterList(prefixList3, RouteFilterListMatchers.rejects(Prefix.parse(prefix1))));
    assertThat(
        c, hasRouteFilterList(prefixList3, RouteFilterListMatchers.rejects(Prefix.parse(prefix2))));

    /* prefix-list p4 should get all addresses from both communities */
    assertThat(
        c,
        hasRouteFilterList(
            "p4",
            allOf(
                permits(Prefix.parse("4.4.4.4/32")),
                permits(Prefix.parse("5.5.5.5/32")),
                RouteFilterListMatchers.rejects(Prefix.parse("1.1.1.1/32")))));

    /* The wildcard-looking BGP group name should not be pruned since its parse-tree node was not created via preprocessor. */
    assertThat(c, hasDefaultVrf(hasBgpProcess(hasNeighbors(hasKey(neighborIp)))));

    /* prefix-list p5 with trailing semicolon in apply-path should exist but be empty */
    assertThat(c, hasRouteFilterLists(hasKey("p5")));
    assertThat(c.getRouteFilterLists().get("p5").getLines(), empty());
    assertThat(c, hasIpAccessList("FILTER"));
    assertThat(c, hasInterface("em0.0", hasIncomingFilter(hasName("FILTER"))));
  }

  @Test
  public void testJuniperWildcardsReference() throws IOException {
    String hostname = "juniper-wildcards";
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    // Confirm definitions are tracked properly for structures defined by apply-groups/apply-path
    assertThat(
        ccae,
        hasDefinedStructureWithDefinitionLines(
            filename, INTERFACE, "lo0", containsInAnyOrder(4, 7, 8)));
    assertThat(
        ccae,
        hasDefinedStructureWithDefinitionLines(
            filename, PREFIX_LIST, "p1", containsInAnyOrder(4, 9, 10, 27)));
    assertThat(
        ccae,
        hasDefinedStructureWithDefinitionLines(
            filename, PREFIX_LIST, "p2", containsInAnyOrder(5, 11)));

    // Confirm undefined references are also tracked properly for apply-groups related references
    assertThat(
        ccae,
        hasUndefinedReferenceWithReferenceLines(
            filename, INTERFACE, "et-0/0/0.0", OSPF_AREA_INTERFACE, containsInAnyOrder(6, 17)));
  }

  /** Test definition and reference tracking for named communities and tunnel attributes. */
  @Test
  public void testJuniperCommunityReference() throws IOException {
    String hostname = "juniper-references-in-policy";
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    // definitions
    assertThat(
        ccae, hasDefinedStructureWithDefinitionLines(filename, COMMUNITY, "MATCHED", contains(4)));
    assertThat(
        ccae,
        hasDefinedStructureWithDefinitionLines(
            filename, COMMUNITY, "UNUSED", containsInAnyOrder(5, 6)));
    assertThat(
        ccae,
        hasDefinedStructureWithDefinitionLines(
            filename, TUNNEL_ATTRIBUTE, "SET", containsInAnyOrder(10, 11)));
    assertThat(
        ccae,
        hasDefinedStructureWithDefinitionLines(
            filename, TUNNEL_ATTRIBUTE, "REMOVE", containsInAnyOrder(12, 13)));

    // defined references
    assertThat(ccae, hasNumReferrers(filename, COMMUNITY, "MATCHED", 1));
    assertThat(ccae, hasNumReferrers(filename, COMMUNITY, "UNUSED", 0));
    assertThat(ccae, hasNumReferrers(filename, COMMUNITY, "ADDED", 1));
    assertThat(ccae, hasNumReferrers(filename, COMMUNITY, "DELETED", 1));
    assertThat(ccae, hasNumReferrers(filename, COMMUNITY, "SET", 1));
    assertThat(ccae, hasNumReferrers(filename, TUNNEL_ATTRIBUTE, "SET", 1));
    assertThat(ccae, hasNumReferrers(filename, TUNNEL_ATTRIBUTE, "REMOVE", 1));
    assertThat(ccae, hasNumReferrers(filename, TUNNEL_ATTRIBUTE, "UNUSED", 0));

    // undefined references
    assertThat(
        ccae,
        hasUndefinedReferenceWithReferenceLines(
            filename, COMMUNITY, "UNDEFINED", POLICY_STATEMENT_FROM_COMMUNITY, contains(18)));
    assertThat(
        ccae,
        hasUndefinedReferenceWithReferenceLines(
            filename,
            TUNNEL_ATTRIBUTE,
            "UNDEFINED",
            POLICY_STATEMENT_THEN_TUNNEL_ATTRIBUTE,
            containsInAnyOrder(23, 25)));
  }

  @Test
  public void testLogicalSystems() throws IOException {
    String snapshotName = "logical-systems";
    String configName = "master1";

    List<String> configurationNames = ImmutableList.of(configName);
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationFiles(TESTRIGS_PREFIX + snapshotName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations(batfish.getSnapshot());

    // There should be 3 configs: the master, and one for each logical system
    // ls1's name should be derived from master hostname and logical-system name
    // ls2's name was manually configured
    assertThat(
        configurations.keySet(),
        containsInAnyOrder(
            configName,
            JuniperConfiguration.computeLogicalSystemDefaultHostname(configName, "ls1"),
            "ls2.example.com"));
  }

  @Test
  public void testLogicalSystemsInterfaces() throws IOException {
    String snapshotName = "logical-systems";
    String configName = "master1";
    String lsConfigName =
        JuniperConfiguration.computeLogicalSystemDefaultHostname(configName, "ls1");

    List<String> configurationNames = ImmutableList.of(configName);
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationFiles(TESTRIGS_PREFIX + snapshotName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations(batfish.getSnapshot());
    Configuration masterConfig = configurations.get(configName);
    Configuration lsConfig = configurations.get(lsConfigName);

    // ensure interfaces have been divided appropriately
    assertThat(
        masterConfig.getAllInterfaces().keySet(),
        containsInAnyOrder("xe-0/0/0", "xe-0/0/0.0", "xe-0/0/1", "xe-0/0/1.1"));
    assertThat(
        lsConfig.getAllInterfaces().keySet(),
        containsInAnyOrder("xe-0/0/1", "xe-0/0/1.0", "xe-0/0/2", "xe-0/0/2.0"));

    // shared physical interface should have same settings on both configs
    assertThat(masterConfig.getAllInterfaces().get("xe-0/0/1.1"), hasMtu(2345));
    assertThat(lsConfig.getAllInterfaces().get("xe-0/0/1.0"), hasMtu(2345));
  }

  @Test
  public void testLogicalSystemsFirewallFilters() throws IOException {
    String snapshotName = "logical-systems";
    String configName = "master1";
    String lsConfigName =
        JuniperConfiguration.computeLogicalSystemDefaultHostname(configName, "ls1");

    List<String> configurationNames = ImmutableList.of(configName);
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationFiles(TESTRIGS_PREFIX + snapshotName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations(batfish.getSnapshot());
    Configuration masterConfig = configurations.get(configName);
    Configuration lsConfig = configurations.get(lsConfigName);

    // ff1 is defined only on master, but should be accessible to both
    assertThat(masterConfig.getIpAccessLists(), hasKey("ff1"));
    assertThat(lsConfig.getIpAccessLists(), hasKey("ff1"));
    assertThat(
        masterConfig.getIpAccessLists().get("ff1"),
        equalTo(lsConfig.getIpAccessLists().get("ff1")));

    // ff2 is defined only on both, but should have different definitions on each
    assertThat(masterConfig.getIpAccessLists(), hasKey("ff2"));
    assertThat(lsConfig.getIpAccessLists(), hasKey("ff2"));
    assertThat(
        masterConfig.getIpAccessLists().get("ff2"),
        not(equalTo(lsConfig.getIpAccessLists().get("ff2"))));

    // ff3 is defined only on logical system, so should be out of scope for master
    assertThat(masterConfig.getIpAccessLists(), not(hasKey("ff3")));
    assertThat(lsConfig.getIpAccessLists(), hasKey("ff3"));
  }

  @Test
  public void testLogicalSystemsPolicyStatements() throws IOException {
    String snapshotName = "logical-systems";
    String configName = "master1";
    String lsConfigName =
        JuniperConfiguration.computeLogicalSystemDefaultHostname(configName, "ls1");

    List<String> configurationNames = ImmutableList.of(configName);
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationFiles(TESTRIGS_PREFIX + snapshotName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations(batfish.getSnapshot());
    Configuration masterConfig = configurations.get(configName);
    Configuration lsConfig = configurations.get(lsConfigName);

    // ps1 is defined only on master, but should be accessible to both
    assertThat(masterConfig.getRoutingPolicies(), hasKey("ps1"));
    assertThat(lsConfig.getRoutingPolicies(), hasKey("ps1"));
    assertThat(
        masterConfig.getRoutingPolicies().get("ps1"),
        equalTo(lsConfig.getRoutingPolicies().get("ps1")));

    // ps2 is defined only on both, but should have dipserent definitions on each
    assertThat(masterConfig.getRoutingPolicies(), hasKey("ps2"));
    assertThat(lsConfig.getRoutingPolicies(), hasKey("ps2"));
    assertThat(
        masterConfig.getRoutingPolicies().get("ps2"),
        not(equalTo(lsConfig.getRoutingPolicies().get("ps2"))));

    // ps3 is defined only on logical system, so should be out of scope for master
    assertThat(masterConfig.getRoutingPolicies(), not(hasKey("ps3")));
    assertThat(lsConfig.getRoutingPolicies(), hasKey("ps3"));
  }

  @Test
  public void testLocalRouteExportBgp() {
    Configuration c = parseConfig("local-route-export-bgp");

    RoutingPolicy peer1RejectAllLocal =
        c.getRoutingPolicies().get(computePeerExportPolicyName(Prefix.parse("1.0.0.1/32")));
    RoutingPolicy peer2RejectPtpLocal =
        c.getRoutingPolicies().get(computePeerExportPolicyName(Prefix.parse("2.0.0.1/32")));
    RoutingPolicy peer3RejectLanLocal =
        c.getRoutingPolicies().get(computePeerExportPolicyName(Prefix.parse("3.0.0.1/32")));
    RoutingPolicy peer4AllowAllLocal =
        c.getRoutingPolicies().get(computePeerExportPolicyName(Prefix.parse("4.0.0.1/32")));

    LocalRoute localRoutePtp =
        new LocalRoute(ConcreteInterfaceAddress.parse("10.0.0.0/31"), "ge-0/0/0.0");
    LocalRoute localRouteLan =
        new LocalRoute(ConcreteInterfaceAddress.parse("10.0.1.1/30"), "ge-0/0/1.0");

    // Peer policies should reject local routes not exported by their VRFs
    Environment.Builder eb = Environment.builder(c).setDirection(Direction.OUT);
    assertThat(
        peer1RejectAllLocal
            .call(
                eb.setOriginalRoute(localRoutePtp).setOutputRoute(Bgpv4Route.testBuilder()).build())
            .getBooleanValue(),
        equalTo(false));
    assertThat(
        peer1RejectAllLocal
            .call(
                eb.setOriginalRoute(localRouteLan).setOutputRoute(Bgpv4Route.testBuilder()).build())
            .getBooleanValue(),
        equalTo(false));

    eb = Environment.builder(c).setDirection(Direction.OUT);
    assertThat(
        peer2RejectPtpLocal
            .call(
                eb.setOriginalRoute(localRoutePtp).setOutputRoute(Bgpv4Route.testBuilder()).build())
            .getBooleanValue(),
        equalTo(false));
    assertThat(
        peer2RejectPtpLocal
            .call(
                eb.setOriginalRoute(localRouteLan).setOutputRoute(Bgpv4Route.testBuilder()).build())
            .getBooleanValue(),
        equalTo(true));

    eb = Environment.builder(c).setDirection(Direction.OUT);
    assertThat(
        peer3RejectLanLocal
            .call(
                eb.setOriginalRoute(localRoutePtp).setOutputRoute(Bgpv4Route.testBuilder()).build())
            .getBooleanValue(),
        equalTo(true));
    assertThat(
        peer3RejectLanLocal
            .call(
                eb.setOriginalRoute(localRouteLan).setOutputRoute(Bgpv4Route.testBuilder()).build())
            .getBooleanValue(),
        equalTo(false));

    eb = Environment.builder(c).setDirection(Direction.OUT);
    assertThat(
        peer4AllowAllLocal
            .call(
                eb.setOriginalRoute(localRoutePtp).setOutputRoute(Bgpv4Route.testBuilder()).build())
            .getBooleanValue(),
        equalTo(true));
    assertThat(
        peer4AllowAllLocal
            .call(
                eb.setOriginalRoute(localRouteLan).setOutputRoute(Bgpv4Route.testBuilder()).build())
            .getBooleanValue(),
        equalTo(true));
  }

  @Test
  public void testLocalRouteExportOspf() {
    Configuration c = parseConfig("local-route-export-ospf");

    String vrf1 = "vrf1";
    RoutingPolicy vrf1RejectAllLocal =
        c.getRoutingPolicies().get(computeOspfExportPolicyName(vrf1));

    String vrf2 = "vrf2";
    RoutingPolicy vrf2RejectPtpLocal =
        c.getRoutingPolicies().get(computeOspfExportPolicyName(vrf2));

    String vrf3 = "vrf3";
    RoutingPolicy vrf3RejectLanLocal =
        c.getRoutingPolicies().get(computeOspfExportPolicyName(vrf3));

    String vrf4 = "vrf4";
    RoutingPolicy vrf4AllowAllLocal = c.getRoutingPolicies().get(computeOspfExportPolicyName(vrf4));

    LocalRoute localRoutePtp =
        new LocalRoute(ConcreteInterfaceAddress.parse("10.0.0.0/31"), "ge-0/0/0.0");
    LocalRoute localRouteLan =
        new LocalRoute(ConcreteInterfaceAddress.parse("10.0.1.1/30"), "ge-0/0/1.0");

    // Peer policies should reject local routes not exported by their VRFs
    Environment.Builder eb = Environment.builder(c).setDirection(Direction.OUT);
    assertThat(
        vrf1RejectAllLocal
            .call(
                eb.setOriginalRoute(localRoutePtp)
                    .setOutputRoute(OspfExternalType2Route.builder())
                    .build())
            .getBooleanValue(),
        equalTo(false));
    assertThat(
        vrf1RejectAllLocal
            .call(
                eb.setOriginalRoute(localRouteLan)
                    .setOutputRoute(OspfExternalType2Route.builder())
                    .build())
            .getBooleanValue(),
        equalTo(false));

    eb = Environment.builder(c).setDirection(Direction.OUT);
    assertThat(
        vrf2RejectPtpLocal
            .call(
                eb.setOriginalRoute(localRoutePtp)
                    .setOutputRoute(OspfExternalType2Route.builder())
                    .build())
            .getBooleanValue(),
        equalTo(false));
    assertThat(
        vrf2RejectPtpLocal
            .call(
                eb.setOriginalRoute(localRouteLan)
                    .setOutputRoute(OspfExternalType2Route.builder())
                    .build())
            .getBooleanValue(),
        equalTo(true));

    eb = Environment.builder(c).setDirection(Direction.OUT);
    assertThat(
        vrf3RejectLanLocal
            .call(
                eb.setOriginalRoute(localRoutePtp)
                    .setOutputRoute(OspfExternalType2Route.builder())
                    .build())
            .getBooleanValue(),
        equalTo(true));
    assertThat(
        vrf3RejectLanLocal
            .call(
                eb.setOriginalRoute(localRouteLan)
                    .setOutputRoute(OspfExternalType2Route.builder())
                    .build())
            .getBooleanValue(),
        equalTo(false));

    eb = Environment.builder(c).setDirection(Direction.OUT);
    assertThat(
        vrf4AllowAllLocal
            .call(
                eb.setOriginalRoute(localRoutePtp)
                    .setOutputRoute(OspfExternalType2Route.builder())
                    .build())
            .getBooleanValue(),
        equalTo(true));
    assertThat(
        vrf4AllowAllLocal
            .call(
                eb.setOriginalRoute(localRouteLan)
                    .setOutputRoute(OspfExternalType2Route.builder())
                    .build())
            .getBooleanValue(),
        equalTo(true));
  }

  @Test
  public void testName() {
    JuniperConfiguration c = parseJuniperConfig("name");
    assertThat(c.getMasterLogicalSystem().getPolicyStatements(), hasKeys("XX"));
    PolicyStatement ps = c.getMasterLogicalSystem().getPolicyStatements().get("XX");
    assertThat(ps.getTerms(), hasKeys("10/8", "Colon:Name", "Dot.Name", "Plus+Name", "Comma,Name"));

    assertThat(
        ((ConcreteFirewallFilter) c.getMasterLogicalSystem().getFirewallFilters().get("filterName"))
            .getTerms(),
        hasKeys("Colon:Name", "Dot.Name", "Slash/Name", "Plus+Name", "Comma,Name"));
  }

  @Test
  public void testNatDest() {
    Configuration config = parseConfig("nat-dest");

    Map<String, Interface> interfaces = config.getAllInterfaces();
    assertThat(interfaces.keySet(), containsInAnyOrder("ge-0/0/0", "ge-0/0/0.0"));

    assertThat(interfaces.get("ge-0/0/0").getIncomingTransformation(), nullValue());

    Interface iface = interfaces.get("ge-0/0/0.0");

    Ip pool1Start = Prefix.parse("10.10.10.10/24").getFirstHostIp();
    Ip pool1End = Prefix.parse("10.10.10.10/24").getLastHostIp();

    Ip pool5Start = Ip.parse("50.50.50.50");
    Ip pool5End = Ip.parse("50.50.50.60");

    Transformation ruleSetRIRule1Transformation =
        when(match(
                HeaderSpace.builder().setSrcPorts(ImmutableList.of(SubRange.singleton(5))).build()))
            .apply(NOOP_DEST_NAT)
            .build();

    Transformation ruleSetZoneRule3Transformation =
        when(match(
                HeaderSpace.builder()
                    .setSrcIps(Prefix.parse("3.3.3.3/24").toIpSpace())
                    .setDstIps(Prefix.parse("1.1.1.1/32").toIpSpace())
                    .build()))
            .apply(assignDestinationIp(pool1Start, pool1End))
            .setOrElse(ruleSetRIRule1Transformation)
            .build();

    Transformation ruleSetZoneRule2Transformation =
        when(match(
                HeaderSpace.builder()
                    .setDstIps(new IpSpaceReference("global~DA-NAME"))
                    .setSrcIps(Prefix.parse("2.2.2.2/24").toIpSpace())
                    .build()))
            .apply(assignDestinationIp(pool5Start, pool5End))
            .setOrElse(ruleSetZoneRule3Transformation)
            .build();

    Transformation ruleSetZoneRule1Transformation =
        when(match(
                HeaderSpace.builder()
                    .setDstIps(new IpSpaceReference("global~NAME"))
                    .setDstPorts(ImmutableList.of(new SubRange(100, 200)))
                    .setSrcPorts(ImmutableList.of(SubRange.singleton(80)))
                    .setSrcIps(new IpSpaceReference("global~SA-NAME"))
                    .build()))
            .apply(NOOP_DEST_NAT)
            .setOrElse(ruleSetZoneRule2Transformation)
            .build();

    Transformation ruleSetIfaceRule3Transformation =
        when(match(
                HeaderSpace.builder()
                    .setIpProtocols(UDP)
                    .setSrcPorts(ImmutableList.of(SubRange.singleton(6)))
                    .build()))
            .apply(NOOP_DEST_NAT)
            .setOrElse(ruleSetZoneRule1Transformation)
            .build();

    assertThat(iface.getIncomingTransformation(), equalTo(ruleSetIfaceRule3Transformation));
  }

  @Test
  public void testNatDestJuniperConfig() {
    JuniperConfiguration config = parseJuniperConfig("nat-dest");

    Nat nat = config.getMasterLogicalSystem().getNatDestination();
    assertThat(nat.getType(), equalTo(Type.DESTINATION));

    // test pools
    Map<String, NatPool> pools = nat.getPools();
    assertThat(
        pools.keySet(), equalTo(ImmutableSet.of("POOL1", "POOL2", "POOL3", "POOL4", "POOL5")));

    NatPool pool1 = pools.get("POOL1");
    assertThat(pool1.getFromAddress(), equalTo(Ip.parse("10.10.10.1")));
    assertThat(pool1.getToAddress(), equalTo(Ip.parse("10.10.10.254")));
    assertThat(pool1.getPortAddressTranslation(), nullValue());

    NatPool pool2 = pools.get("POOL2");
    assertThat(pool2.getFromAddress(), equalTo(Ip.parse("20.20.20.1")));
    assertThat(pool2.getToAddress(), equalTo(Ip.parse("20.20.20.254")));
    assertThat(pool2.getPortAddressTranslation(), instanceOf(PatPool.class));
    PatPool pat2 = (PatPool) pool2.getPortAddressTranslation();
    assertThat(pat2.getToPort(), allOf(equalTo(pat2.getFromPort()), equalTo(22)));

    NatPool pool3 = pools.get("POOL3");
    assertThat(pool3.getFromAddress(), equalTo(Ip.parse("30.30.30.30")));
    assertThat(pool3.getToAddress(), equalTo(Ip.parse("30.30.30.30")));
    assertThat(pool3.getPortAddressTranslation(), nullValue());

    NatPool pool4 = pools.get("POOL4");
    assertThat(pool4.getFromAddress(), equalTo(Ip.parse("40.40.40.40")));
    assertThat(pool4.getToAddress(), equalTo(Ip.parse("40.40.40.40")));
    assertThat(pool4.getPortAddressTranslation(), instanceOf(PatPool.class));
    PatPool pat4 = (PatPool) pool4.getPortAddressTranslation();
    assertThat(pat4.getToPort(), allOf(equalTo(pat4.getFromPort()), equalTo(44)));

    NatPool pool5 = pools.get("POOL5");
    assertThat(pool5.getFromAddress(), equalTo(Ip.parse("50.50.50.50")));
    assertThat(pool5.getToAddress(), equalTo(Ip.parse("50.50.50.60")));
    assertThat(pool5.getPortAddressTranslation(), nullValue());

    // test rule sets
    Map<String, NatRuleSet> ruleSets = nat.getRuleSets();
    assertThat(
        ruleSets.keySet(), containsInAnyOrder("RULE-SET-RI", "RULE-SET-ZONE", "RULE-SET-IFACE"));

    // test fromLocations
    NatPacketLocation fromLocation = ruleSets.get("RULE-SET-IFACE").getFromLocation();
    assertThat(fromLocation.getInterface(), equalTo("ge-0/0/0.0"));
    assertThat(fromLocation.getRoutingInstance(), nullValue());
    assertThat(fromLocation.getZone(), nullValue());
    List<NatRule> rules = ruleSets.get("RULE-SET-IFACE").getRules();
    assertThat(rules, hasSize(1));
    NatRule rule1 = rules.get(0);
    assertThat(rule1.getName(), equalTo("RULE1"));
    assertThat(
        rule1.getMatches(), contains(new NatRuleMatchSrcPort(6, 6), new NatRuleMatchProtocol(UDP)));
    assertThat(rule1.getThen(), equalTo(NatRuleThenOff.INSTANCE));

    fromLocation = ruleSets.get("RULE-SET-RI").getFromLocation();
    assertThat(fromLocation.getInterface(), nullValue());
    assertThat(fromLocation.getRoutingInstance(), equalTo("RI"));
    assertThat(fromLocation.getZone(), nullValue());

    fromLocation = ruleSets.get("RULE-SET-ZONE").getFromLocation();
    assertThat(fromLocation.getInterface(), nullValue());
    assertThat(fromLocation.getRoutingInstance(), nullValue());
    assertThat(fromLocation.getZone(), equalTo("ZONE"));

    // test RULE-SET-ZONE rules
    rules = ruleSets.get("RULE-SET-ZONE").getRules();
    assertThat(rules, hasSize(3));

    // test rule1
    rule1 = rules.get(0);
    assertThat(rule1.getName(), equalTo("RULE1"));
    assertThat(
        rule1.getMatches(),
        contains(
            new NatRuleMatchSrcPort(80, 80),
            new NatRuleMatchDstPort(100, 200),
            new NatRuleMatchDstAddrName("NAME"),
            new NatRuleMatchSrcAddrName("SA-NAME")));
    assertThat(rule1.getThen(), equalTo(NatRuleThenOff.INSTANCE));

    // test rule2
    NatRule rule2 = rules.get(1);
    assertThat(rule2.getName(), equalTo("RULE2"));
    assertThat(
        rule2.getMatches(),
        contains(
            new NatRuleMatchSrcAddr(Prefix.parse("2.2.2.2/24")),
            new NatRuleMatchDstAddrName("DA-NAME")));
    assertThat(rule2.getThen(), equalTo(new NatRuleThenPool("POOL5")));

    // test rule3
    NatRule rule3 = rules.get(2);
    assertThat(rule3.getName(), equalTo("RULE3"));
    assertThat(
        rule3.getMatches(),
        contains(
            new NatRuleMatchSrcAddr(Prefix.parse("3.3.3.3/24")),
            new NatRuleMatchDstAddr(Prefix.parse("1.1.1.1/32"))));
    assertThat(rule3.getThen(), equalTo(new NatRuleThenPool("POOL1")));
  }

  @Test
  public void testNatSource() {
    Configuration config = parseConfig("nat-source2");
    Map<String, Interface> interfaceMap = config.getAllInterfaces();

    assertThat(
        interfaceMap.keySet(),
        containsInAnyOrder("ge-0/0/0.0", "ge-0/0/1.0", "ge-0/0/0", "ge-0/0/1"));

    Interface iface0 = interfaceMap.get("ge-0/0/0.0");
    Interface iface1 = interfaceMap.get("ge-0/0/1.0");

    assertThat(iface0.getOutgoingTransformation(), nullValue());

    // rule set 1 has a routing instance from location, so it goes last
    AssignIpAddressFromPool transformationStep =
        assignSourceIp(Ip.parse("10.10.10.1"), Ip.parse("10.10.10.254"));

    AssignPortFromPool portTransformationStep =
        assignSourcePort(Nat.DEFAULT_FROM_PORT, Nat.DEFAULT_TO_PORT);

    Transformation ruleSet1Transformation =
        when(matchSrcInterface("ge-0/0/0.0"))
            .setAndThen(
                when(match(
                        HeaderSpace.builder()
                            .setDstIps(Prefix.parse("1.1.1.1/24").toIpSpace())
                            .build()))
                    .apply(transformationStep, portTransformationStep)
                    .build())
            .build();

    // rule set 3 has a zone from location, so it goes second
    Transformation ruleSet3Transformation =
        when(matchSrcInterface("ge-0/0/0.0", "ge-0/0/1.0"))
            .setAndThen(
                when(match(
                        HeaderSpace.builder()
                            .setDstIps(Prefix.parse("3.3.3.3/24").toIpSpace())
                            .build()))
                    .apply(transformationStep, portTransformationStep)
                    .setOrElse(ruleSet1Transformation)
                    .build())
            .setOrElse(ruleSet1Transformation)
            .build();

    // rule set 2 has an interface from location, so it goes first
    Transformation ruleSet2Transformation =
        when(matchSrcInterface("ge-0/0/0.0"))
            .setAndThen(
                when(match(
                        HeaderSpace.builder()
                            .setDstIps(Prefix.parse("2.2.2.2/24").toIpSpace())
                            .build()))
                    .apply(transformationStep, portTransformationStep)
                    .setOrElse(
                        // routing instance rule set
                        ruleSet3Transformation)
                    .build())
            .setOrElse(ruleSet3Transformation)
            .build();

    assertThat(iface1.getOutgoingTransformation(), equalTo(ruleSet2Transformation));
  }

  @Test
  public void testNatSourceJuniperConfig() {
    JuniperConfiguration config = parseJuniperConfig("nat-source");

    Nat nat = config.getMasterLogicalSystem().getNatSource();
    assertThat(nat.getType(), equalTo(Type.SOURCE));

    // test pools
    Map<String, NatPool> pools = nat.getPools();
    assertThat(pools.keySet(), containsInAnyOrder("POOL1", "POOL2"));

    NatPool pool1 = pools.get("POOL1");
    Ip ip1 = Ip.parse("10.10.10.1");
    Ip ip2 = Ip.parse("10.10.10.254");
    assertThat(pool1.getFromAddress(), equalTo(ip1));
    assertThat(pool1.getToAddress(), equalTo(ip2));

    NatPool pool2 = pools.get("POOL2");
    assertThat(pool2.getFromAddress(), equalTo(Ip.parse("10.10.10.10")));
    assertThat(pool2.getToAddress(), equalTo(Ip.parse("10.10.10.20")));

    // test rule sets
    Map<String, NatRuleSet> ruleSets = nat.getRuleSets();
    assertThat(ruleSets.keySet(), contains("RULE-SET"));

    NatRuleSet ruleSet = ruleSets.get("RULE-SET");

    /*
     * test from location lines -- it doesn't make sense to have more than one of these, but the
     * extraction supports it.
     */
    assertThat(ruleSet.getFromLocation().getInterface(), nullValue());
    assertThat(ruleSet.getFromLocation().getRoutingInstance(), nullValue());
    assertThat(ruleSet.getFromLocation().getZone(), equalTo("FROM-ZONE"));

    // test to location lines
    assertThat(ruleSet.getToLocation().getInterface(), nullValue());
    assertThat(ruleSet.getToLocation().getRoutingInstance(), nullValue());
    assertThat(ruleSet.getToLocation().getZone(), equalTo("TO-ZONE"));

    // test rules
    List<NatRule> rules = ruleSet.getRules();
    assertThat(rules, hasSize(3));

    // test rule1
    NatRule rule1 = rules.get(0);
    assertThat(rule1.getName(), equalTo("RULE1"));
    assertThat(
        rule1.getMatches(),
        equalTo(
            ImmutableList.of(
                new NatRuleMatchDstAddr(Prefix.parse("1.1.1.1/24")),
                new NatRuleMatchDstAddrName("NAME"))));
    assertThat(rule1.getThen(), equalTo(NatRuleThenOff.INSTANCE));

    // test rule2
    NatRule rule2 = rules.get(1);
    assertThat(rule2.getName(), equalTo("RULE2"));
    assertThat(
        rule2.getMatches(),
        equalTo(
            ImmutableList.of(
                new NatRuleMatchSrcAddr(Prefix.parse("2.2.2.2/24")),
                new NatRuleMatchSrcAddrName("SA-NAME"))));
    assertThat(rule2.getThen(), equalTo(new NatRuleThenPool("POOL")));

    // test rule3
    NatRule rule3 = rules.get(2);
    assertThat(rule3.getName(), equalTo("RULE3"));
    assertThat(
        rule3.getMatches(),
        equalTo(ImmutableList.of(new NatRuleMatchSrcAddr(Prefix.parse("3.3.3.0/24")))));
    assertThat(rule3.getThen(), equalTo(NatRuleThenInterface.INSTANCE));
  }

  @Test
  public void testNatSourceJuniperConfig2() {
    JuniperConfiguration config = parseJuniperConfig("nat-source2");

    Nat nat = config.getMasterLogicalSystem().getNatSource();
    assertThat(nat.getType(), equalTo(Type.SOURCE));

    Map<String, NatPool> pools = nat.getPools();
    assertThat(pools.keySet(), contains("POOL1"));

    NatPool pool1 = pools.get("POOL1");
    Ip ip1 = Ip.parse("10.10.10.1");
    Ip ip2 = Ip.parse("10.10.10.254");
    assertThat(pool1.getFromAddress(), equalTo(ip1));
    assertThat(pool1.getToAddress(), equalTo(ip2));

    // test rule sets
    Map<String, NatRuleSet> ruleSets = nat.getRuleSets();
    assertThat(ruleSets.keySet(), contains("RULE-SET1", "RULE-SET2", "RULE-SET3"));

    NatRuleSet ruleSet = ruleSets.get("RULE-SET1");
    assertThat(ruleSet.getFromLocation().getRoutingInstance(), equalTo("RI"));
    assertThat(ruleSet.getToLocation().getInterface(), equalTo("ge-0/0/1.0"));

    NatRuleSet ruleSet2 = ruleSets.get("RULE-SET2");
    assertThat(ruleSet2.getFromLocation().getInterface(), equalTo("ge-0/0/0.0"));
    assertThat(ruleSet2.getToLocation().getInterface(), equalTo("ge-0/0/1.0"));

    // test rules
    List<NatRule> rules = ruleSet.getRules();
    assertThat(rules, hasSize(1));

    NatRule rule1 = rules.get(0);
    assertThat(rule1.getName(), equalTo("RULE1"));
    assertThat(
        rule1.getMatches(),
        equalTo(ImmutableList.of(new NatRuleMatchDstAddr(Prefix.parse("1.1.1.1/24")))));
    assertThat(rule1.getThen(), equalTo(new NatRuleThenPool("POOL1")));

    rules = ruleSet2.getRules();
    assertThat(rules, hasSize(1));

    NatRule rule2 = rules.get(0);
    assertThat(rule2.getName(), equalTo("RULE1"));
    assertThat(
        rule2.getMatches(),
        equalTo(ImmutableList.of(new NatRuleMatchDstAddr(Prefix.parse("2.2.2.2/24")))));
    assertThat(rule2.getThen(), equalTo(new NatRuleThenPool("POOL1")));
  }

  @Test
  public void testAsnLeadingZerosExtraction() {
    JuniperConfiguration vc = parseJuniperConfig("asn-leading-zeros");
    Map<Prefix, IpBgpGroup> peers =
        vc.getMasterLogicalSystem().getRoutingInstances().get("ri1").getIpBgpGroups();
    assertThat(peers.get(Prefix.strict("192.0.2.1/32")).getPeerAs(), equalTo((1L << 16) + 2));
    assertThat(peers.get(Prefix.strict("192.0.2.2/32")).getPeerAs(), equalTo((3L << 16) + 4));
    assertThat(peers.get(Prefix.strict("192.0.2.3/32")).getPeerAs(), equalTo((5L << 16) + 6));
    assertThat(peers.get(Prefix.strict("192.0.2.4/32")).getPeerAs(), equalTo((7L << 16) + 8));
    assertThat(peers.get(Prefix.strict("192.0.2.5/32")).getPeerAs(), equalTo((4000000000L)));
  }

  @Test
  public void testOspfInterfaceAreaAssignment() {
    Configuration c = parseConfig("ospfInterfaceAreaAssignment");

    /* Properly configured interfaces should be present in respective areas. */
    assertThat(c, hasInterface("xe-0/0/0.0", hasOspfAreaName(0L)));
    assertThat(c, hasInterface("xe-0/0/0.0", isOspfPassive(equalTo(false))));
    assertThat(c.getAllInterfaces(), not(hasKey("ge-0/0/0.1")));
    assertThat(
        c,
        hasDefaultVrf(
            hasOspfProcess(
                DEFAULT_VRF_NAME,
                hasArea(0L, OspfAreaMatchers.hasInterfaces(hasItem("xe-0/0/0.0"))))));

    assertThat(c, hasInterface("xe-0/0/0.1", hasOspfAreaName(1L)));
    assertThat(c, hasInterface("xe-0/0/0.1", isOspfPassive()));
    assertThat(
        c,
        hasDefaultVrf(
            hasOspfProcess(
                DEFAULT_VRF_NAME,
                hasArea(1L, OspfAreaMatchers.hasInterfaces(hasItem("xe-0/0/0.1"))))));

    /* The following interfaces should be absent since they have no IP addresses assigned. */
    assertThat(c, hasInterface("xe-0/0/0.2", hasOspfAreaName(nullValue())));
    assertThat(c, hasDefaultVrf(hasOspfProcess(DEFAULT_VRF_NAME, notNullValue())));
    OspfProcess proc = c.getVrfs().get(DEFAULT_VRF_NAME).getOspfProcesses().get(DEFAULT_VRF_NAME);
    assertThat(proc, hasArea(0L, OspfAreaMatchers.hasInterfaces(not(hasItem("xe-0/0/0.2")))));
    assertThat(proc, hasArea(0L, OspfAreaMatchers.hasInterfaces(not(hasItem("xe-0/0/0.3")))));
    assertThat(proc, hasArea(1L, OspfAreaMatchers.hasInterfaces(not(hasItem("xe-0/0/0.3")))));
  }

  @Test
  public void testOspfRouterId() {
    Configuration c = parseConfig("ospf-router-id");

    assertThat(
        c,
        hasDefaultVrf(hasOspfProcess(DEFAULT_VRF_NAME, hasRouterId(equalTo(Ip.parse("1.0.0.1"))))));
  }

  @Test
  public void testOspfSummaries() {
    Configuration c = parseConfig(("ospf-abr-with-summaries"));

    assertThat(
        c,
        hasDefaultVrf(
            hasOspfProcess(
                DEFAULT_VRF_NAME,
                hasArea(
                    1L,
                    allOf(
                        hasStubType(equalTo(StubType.STUB)),
                        hasSummary(Prefix.parse("10.0.1.0/24"), isAdvertised()))))));
    String summaryFilterName =
        c.getDefaultVrf()
            .getOspfProcesses()
            .get(DEFAULT_VRF_NAME)
            .getAreas()
            .get(1L)
            .getSummaryFilter();
    assertThat(summaryFilterName, not(nullValue()));
    assertThat(c.getRouteFilterLists().get(summaryFilterName), not(nullValue()));
    Prefix blockPrefix = Prefix.parse("10.0.1.0/24");
    assertThat(
        c.getRouteFilterLists().get(summaryFilterName).getLines(),
        equalTo(
            ImmutableList.of(
                new RouteFilterLine(
                    LineAction.DENY,
                    blockPrefix,
                    new SubRange(blockPrefix.getPrefixLength() + 1, Prefix.MAX_PREFIX_LENGTH)),
                new RouteFilterLine(
                    LineAction.PERMIT, Prefix.ZERO, new SubRange(0, Prefix.MAX_PREFIX_LENGTH)))));
  }

  @Test
  public void testOverlayEcmp() throws IOException {
    String hostname = "juniper-overlay-ecmp";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    List<ParseWarning> parseWarnings =
        batfish
            .loadParseVendorConfigurationAnswerElement(batfish.getSnapshot())
            .getWarnings()
            .get("configs/" + hostname)
            .getParseWarnings();
    assertThat(
        parseWarnings,
        hasItem(
            allOf(
                hasComment("This feature is not currently supported"),
                hasText("vxlan-routing overlay-ecmp"))));
  }

  @Test
  public void testParsingRecovery() {
    String recoveryText = readResource("org/batfish/grammar/juniper/testconfigs/recovery", UTF_8);
    Settings settings = new Settings();
    FlatJuniperCombinedParser cp = new FlatJuniperCombinedParser(recoveryText, settings);
    Flat_juniper_configurationContext ctx = cp.parse();
    FlatJuniperRecoveryExtractor extractor = new FlatJuniperRecoveryExtractor();
    ParseTreeWalker walker = new BatfishParseTreeWalker(cp);
    walker.walk(extractor, ctx);

    assertThat(extractor.getNumSets(), equalTo(9));
    assertThat(extractor.getNumErrorNodes(), equalTo(7));
  }

  @Test
  public void testPredefinedJunosApplications() throws IOException {
    Batfish batfish = getBatfishForConfigurationNames("pre-defined-junos-applications");
    InitInfoAnswerElement answer = batfish.initInfo(batfish.getSnapshot(), false, true);
    assertThat(
        answer.toString(),
        not(Matchers.containsString("unimplemented pre-defined junos application")));
  }

  /**
   * Tests that all parsed applications are converted to JunosApplication (in {@link
   * ConfigurationBuilder})
   */
  @Test
  public void testPredefinedJunosApplicationsConverted() throws IOException {
    // conversion failure will cause an exception
    String hostname = "pre-defined-junos-applications-converted";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());
    assertThat(ccae.getUndefinedReferences().get("configs/" + hostname), anEmptyMap());
  }

  @Test
  public void testPredefinedJunosApplicationSets() throws IOException {
    Batfish batfish = getBatfishForConfigurationNames("pre-defined-junos-application-sets");
    InitInfoAnswerElement answer = batfish.initInfo(batfish.getSnapshot(), false, true);
    assertThat(
        answer.toString(),
        not(Matchers.containsString("unimplemented pre-defined junos application-set")));
  }

  @Test
  public void testPrefixList() throws IOException {
    String hostname = "prefix-lists";
    String filename = "configs/" + hostname;
    Configuration c = parseConfig(hostname);
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    Flow flowAccepted1 = createFlow("1.2.3.4", "0.0.0.0");
    Flow flowAccepted2 = createFlow("1.2.3.5", "0.0.0.0");
    Flow flowDenied = createFlow("9.8.7.6", "0.0.0.0");

    IpAccessList filterPrefixList = c.getIpAccessLists().get("FILTER_PL");

    // Confirm referrers are tracked properly
    assertThat(ccae, hasNumReferrers(filename, PREFIX_LIST, "PL", 5));
    assertThat(ccae, hasNumReferrers(filename, PREFIX_LIST, "PL_UNUSED", 0));

    // Confirm undefined reference is detected
    assertThat(ccae, hasUndefinedReference(filename, PREFIX_LIST, "PL_UNDEF"));

    // Only flow from accepted source-prefixes should be accepted
    assertThat(filterPrefixList, rejects(flowDenied, null, c));
    assertThat(filterPrefixList, accepts(flowAccepted1, null, c));
    assertThat(filterPrefixList, accepts(flowAccepted2, null, c));
  }

  @Test
  public void testRtfPrefixList() throws IOException {
    String hostname = "rtf-prefix-lists";
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    // Verify that the rtf-prefix-list is properly tracked as a defined structure
    assertThat(ccae, hasDefinedStructure(filename, RTF_PREFIX_LIST, "RTF_PL"));
    assertThat(ccae, hasDefinedStructure(filename, RTF_PREFIX_LIST, "RTF_PL_UNUSED"));

    // Verify that the reference to the rtf-prefix-list is properly tracked
    assertThat(
        ccae,
        hasReferencedStructure(
            filename, RTF_PREFIX_LIST, "RTF_PL", POLICY_STATEMENT_RTF_PREFIX_LIST));
    assertThat(ccae, hasNumReferrers(filename, RTF_PREFIX_LIST, "RTF_PL", 1));
    assertThat(ccae, hasNumReferrers(filename, RTF_PREFIX_LIST, "RTF_PL_UNUSED", 0));

    // Verify that the undefined reference is detected
    assertThat(ccae, hasUndefinedReference(filename, RTF_PREFIX_LIST, "RTF_PL_UNDEF"));
  }

  @Test
  public void testPrefixListEmpty() {
    Configuration c = parseConfig("prefix-list-empty");
    Flow testFlow1 = createFlow("9.8.7.6", "0.0.0.0");
    Flow testFlow2 = createFlow("1.2.3.4", "1.2.3.4");
    Flow testFlow3 = createFlow("0.0.0.0", "9.8.7.6");

    IpAccessList incomingFilterSource = c.getIpAccessLists().get("TEST_FILTER_SOURCE");
    IpAccessList incomingFilterSourceExcept = c.getIpAccessLists().get("TEST_FILTER_SOURCE_EXCEPT");

    IpAccessList incomingFilterDestination = c.getIpAccessLists().get("TEST_FILTER_DESTINATION");
    IpAccessList incomingFilterDestinationExcept =
        c.getIpAccessLists().get("TEST_FILTER_DESTINATION_EXCEPT");

    IpAccessList incomingFilter = c.getIpAccessLists().get("TEST_FILTER");

    // No source IP should match the empty prefix list
    assertThat(incomingFilterSource, rejects(testFlow1, null, c));
    assertThat(incomingFilterSource, rejects(testFlow2, null, c));
    assertThat(incomingFilterSource, rejects(testFlow3, null, c));

    // Every source IP should match the empty prefix list
    assertThat(incomingFilterSourceExcept, accepts(testFlow1, null, c));
    assertThat(incomingFilterSourceExcept, accepts(testFlow2, null, c));
    assertThat(incomingFilterSourceExcept, accepts(testFlow3, null, c));

    // No destination IP should match the empty prefix list
    assertThat(incomingFilterDestination, rejects(testFlow1, null, c));
    assertThat(incomingFilterDestination, rejects(testFlow2, null, c));
    assertThat(incomingFilterDestination, rejects(testFlow3, null, c));

    // Every destination IP should match the empty prefix list
    assertThat(incomingFilterDestinationExcept, accepts(testFlow1, null, c));
    assertThat(incomingFilterDestinationExcept, accepts(testFlow2, null, c));
    assertThat(incomingFilterDestinationExcept, accepts(testFlow3, null, c));

    // No dest or source IP should match the empty prefix list
    assertThat(incomingFilter, rejects(testFlow1, null, c));
    assertThat(incomingFilter, rejects(testFlow2, null, c));
    assertThat(incomingFilter, rejects(testFlow3, null, c));
  }

  @Test
  public void testGH6149Preprocess() {
    Configuration c = parseConfig("gh-6149-preprocess");
    assertThat(
        c,
        allOf(
            hasInterface("ae1.0"),
            hasIpAccessList("ae1.0-i"),
            hasIpAccessList("filterA"),
            hasIpAccessList("filterB")));
    Interface ae1_0 = c.getAllInterfaces().get("ae1.0");
    // The interface gets the Juniper-standard name for a composite input filter.
    assertThat(ae1_0, hasIncomingFilter(hasName("ae1.0-i")));
    // The ACL has the correct lines.
    List<AclLine> lines = c.getIpAccessLists().get("ae1.0-i").getLines();
    assertThat(lines, contains(instanceOf(AclAclLine.class), instanceOf(AclAclLine.class)));
    AclAclLine line0 = (AclAclLine) lines.get(0);
    assertThat(line0.getAclName(), equalTo("filterA"));
    AclAclLine line1 = (AclAclLine) lines.get(1);
    assertThat(line1.getAclName(), equalTo("filterB"));
  }

  @Test
  public void testGH6307() {
    parseConfig("gh-6307");
    // don't crash.
  }

  @Test
  public void testGH8744() {
    parseConfig("gh-8744");
    // don't crash.
  }

  @Test
  public void testImplicitInitInterface() {
    JuniperConfiguration juniperConfiguration = parseJuniperConfig("implicit-init-interface");

    String phyIfaceName = "ge-1/1/0";
    String unitIfaceName = phyIfaceName + ".1001";
    Map<String, org.batfish.representation.juniper.Interface> interfaces =
        juniperConfiguration.getMasterLogicalSystem().getInterfaces();

    assertThat(interfaces.keySet(), equalTo(ImmutableSet.of(phyIfaceName)));
    org.batfish.representation.juniper.Interface phyIface = interfaces.get(phyIfaceName);

    assertThat(phyIface.getUnits().keySet(), equalTo(ImmutableSet.of(unitIfaceName)));
    org.batfish.representation.juniper.Interface unitIface = phyIface.getUnits().get(unitIfaceName);

    assertThat(unitIface.getParent(), equalTo(phyIface));
  }

  @Test
  public void testRouteFilters() {
    Configuration c = parseConfig("route-filter");
    RouteFilterList rfl = c.getRouteFilterLists().get("route-filter-test:t1");
    assertThat(
        rfl,
        RouteFilterListMatchers.hasLines(
            containsInAnyOrder(
                new RouteFilterLine(
                    LineAction.PERMIT, Prefix.parse("1.2.0.0/16"), SubRange.singleton(16)),
                new RouteFilterLine(
                    LineAction.PERMIT, Prefix.parse("1.2.0.0/16"), new SubRange(17, 32)),
                new RouteFilterLine(
                    LineAction.PERMIT, Prefix.parse("1.7.0.0/16"), SubRange.singleton(16)),
                new RouteFilterLine(
                    LineAction.PERMIT, Prefix.parse("1.7.0.0/17"), SubRange.singleton(17)),
                new RouteFilterLine(
                    LineAction.PERMIT,
                    IpWildcard.parse("1.0.0.0:0.255.0.255"),
                    SubRange.singleton(16)))));
  }

  @Test
  public void testRoutingInstanceType() {
    Configuration c = parseConfig("routing-instance-type");

    /* All types for now should result in a VRF */
    /* TODO: perhaps some types e.g. forwarding should not result in a VRF */
    assertThat(c, hasVrfs(hasKey("ri-forwarding")));
    assertThat(c, hasVrfs(hasKey("ri-l2vpn")));
    assertThat(c, hasVrfs(hasKey("ri-virtual-router")));
    assertThat(c, hasVrfs(hasKey("ri-virtual-switch")));
    assertThat(c, hasVrfs(hasKey("ri-vrf")));
  }

  @Test
  public void testRoutingOptionsMiscParsing() {
    // Just don't have parse warnings, for now.
    parseJuniperConfig("routing-options-misc");
  }

  @Test
  public void testRibNamesMiscParsing() {
    // Just don't have parse warnings
    parseJuniperConfig("routing-options-misc");
  }

  @Test
  public void testRoutingPolicy() {
    Configuration c = parseConfig("routing-policy");

    RoutingPolicy policyExact = c.getRoutingPolicies().get("route-filter-exact");
    RoutingPolicy policyLonger = c.getRoutingPolicies().get("route-filter-longer");
    RoutingPolicy policyPrange = c.getRoutingPolicies().get("route-filter-prange");
    RoutingPolicy policyThrough = c.getRoutingPolicies().get("route-filter-through");
    RoutingPolicy policyAddressmask = c.getRoutingPolicies().get("route-filter-addressmask");

    ConnectedRoute connectedRouteExact =
        new ConnectedRoute(Prefix.parse("1.2.3.4/16"), "nhinttest");
    ConnectedRoute connectedRouteLonger =
        new ConnectedRoute(Prefix.parse("1.2.3.4/19"), "nhinttest");
    ConnectedRoute connectedRouteInRange =
        new ConnectedRoute(Prefix.parse("1.2.3.4/17"), "nhinttest");
    ConnectedRoute connectedRouteInvalidPrefix =
        new ConnectedRoute(Prefix.parse("2.3.3.4/17"), "nhinttest");
    ConnectedRoute connectedRouteInvalidLength =
        new ConnectedRoute(Prefix.parse("2.3.3.4/29"), "nhinttest");

    ConnectedRoute connectedRouteMaskInvalidPrefix =
        new ConnectedRoute(Prefix.parse("9.2.9.4/16"), "nhinttest");
    ConnectedRoute connectedRouteMaskValid =
        new ConnectedRoute(Prefix.parse("1.9.3.9/16"), "nhinttest");
    ConnectedRoute connectedRouteMaskInvalidLength =
        new ConnectedRoute(Prefix.parse("1.9.3.9/17"), "nhinttest");

    Environment.Builder eb = Environment.builder(c).setDirection(IN);

    assertThat(
        policyExact.call(eb.setOriginalRoute(connectedRouteExact).build()).getBooleanValue(),
        equalTo(true));
    assertThat(
        policyExact.call(eb.setOriginalRoute(connectedRouteLonger).build()).getBooleanValue(),
        equalTo(false));
    assertThat(
        policyExact
            .call(eb.setOriginalRoute(connectedRouteInvalidPrefix).build())
            .getBooleanValue(),
        equalTo(false));

    assertThat(
        policyLonger.call(eb.setOriginalRoute(connectedRouteLonger).build()).getBooleanValue(),
        equalTo(true));
    assertThat(
        policyLonger
            .call(eb.setOriginalRoute(connectedRouteInvalidLength).build())
            .getBooleanValue(),
        equalTo(false));
    assertThat(
        policyLonger
            .call(eb.setOriginalRoute(connectedRouteInvalidPrefix).build())
            .getBooleanValue(),
        equalTo(false));

    assertThat(
        policyPrange.call(eb.setOriginalRoute(connectedRouteInRange).build()).getBooleanValue(),
        equalTo(true));
    assertThat(
        policyPrange.call(eb.setOriginalRoute(connectedRouteLonger).build()).getBooleanValue(),
        equalTo(false));
    assertThat(
        policyPrange
            .call(eb.setOriginalRoute(connectedRouteInvalidPrefix).build())
            .getBooleanValue(),
        equalTo(false));

    assertThat(
        policyThrough.call(eb.setOriginalRoute(connectedRouteInRange).build()).getBooleanValue(),
        equalTo(true));
    assertThat(
        policyThrough.call(eb.setOriginalRoute(connectedRouteLonger).build()).getBooleanValue(),
        equalTo(false));
    assertThat(
        policyThrough
            .call(eb.setOriginalRoute(connectedRouteInvalidPrefix).build())
            .getBooleanValue(),
        equalTo(false));

    assertThat(
        policyAddressmask
            .call(eb.setOriginalRoute(connectedRouteMaskValid).build())
            .getBooleanValue(),
        equalTo(true));
    assertThat(
        policyAddressmask
            .call(eb.setOriginalRoute(connectedRouteMaskInvalidPrefix).build())
            .getBooleanValue(),
        equalTo(false));
    assertThat(
        policyAddressmask
            .call(eb.setOriginalRoute(connectedRouteMaskInvalidLength).build())
            .getBooleanValue(),
        equalTo(false));
  }

  @Test
  public void testMatchFromProtocols() {
    Configuration c = parseConfig("from-protocols");
    RoutingPolicy fromIsis = c.getRoutingPolicies().get("from-isis");
    RoutingPolicy fromOspf = c.getRoutingPolicies().get("from-ospf");

    // Build IS-IS and OSPF routes of every protocol
    IsisRoute.Builder isisBuilder =
        IsisRoute.builder()
            .setArea("area")
            .setNetwork(Prefix.ZERO)
            .setNextHopIp(UNSET_ROUTE_NEXT_HOP_IP)
            .setSystemId("systemId");
    IsisRoute isisL1 =
        isisBuilder.setLevel(IsisLevel.LEVEL_1).setProtocol(RoutingProtocol.ISIS_L1).build();
    IsisRoute isisL2 =
        isisBuilder.setLevel(IsisLevel.LEVEL_2).setProtocol(RoutingProtocol.ISIS_L2).build();
    IsisRoute isisEl1 =
        isisBuilder.setLevel(IsisLevel.LEVEL_1).setProtocol(RoutingProtocol.ISIS_EL1).build();
    IsisRoute isisEl2 =
        isisBuilder.setLevel(IsisLevel.LEVEL_2).setProtocol(RoutingProtocol.ISIS_EL2).build();
    OspfRoute ospfIntra =
        OspfIntraAreaRoute.builder()
            .setNetwork(Prefix.ZERO)
            .setNextHop(NextHopDiscard.instance())
            .setArea(1)
            .build();
    OspfRoute ospfInter =
        OspfInterAreaRoute.builder()
            .setNetwork(Prefix.ZERO)
            .setNextHop(NextHopDiscard.instance())
            .setArea(1)
            .build();
    OspfRoute ospfE1 =
        OspfExternalType1Route.builder()
            .setNetwork(Prefix.ZERO)
            .setNextHop(NextHopDiscard.instance())
            .setLsaMetric(1)
            .setArea(1)
            .setCostToAdvertiser(1)
            .setAdvertiser("")
            .build();
    OspfRoute ospfE2 =
        OspfExternalType2Route.builder()
            .setNetwork(Prefix.ZERO)
            .setNextHop(NextHopDiscard.instance())
            .setLsaMetric(1)
            .setArea(1)
            .setCostToAdvertiser(1)
            .setAdvertiser("")
            .build();

    // Make sure all the OSPF protocols are as expected
    assertThat(ospfIntra.getProtocol(), equalTo(RoutingProtocol.OSPF));
    assertThat(ospfInter.getProtocol(), equalTo(RoutingProtocol.OSPF_IA));
    assertThat(ospfE1.getProtocol(), equalTo(RoutingProtocol.OSPF_E1));
    assertThat(ospfE2.getProtocol(), equalTo(RoutingProtocol.OSPF_E2));

    // "from protocol isis" should match any type of IS-IS route
    assertTrue(fromIsis.process(isisL1, IsisRoute.builder(), IN));
    assertTrue(fromIsis.process(isisL2, IsisRoute.builder(), IN));
    assertTrue(fromIsis.process(isisEl1, IsisRoute.builder(), IN));
    assertTrue(fromIsis.process(isisEl2, IsisRoute.builder(), IN));

    // "from protocol ospf" should match any type of OSPF route
    assertTrue(fromOspf.process(ospfIntra, ospfIntra.toBuilder(), IN));
    assertTrue(fromOspf.process(ospfInter, ospfInter.toBuilder(), IN));
    assertTrue(fromOspf.process(ospfE1, ospfE1.toBuilder(), IN));
    assertTrue(fromOspf.process(ospfE2, ospfE2.toBuilder(), IN));

    // Neither policy should match routes of other protocols
    assertFalse(fromIsis.process(ospfIntra, IsisRoute.builder(), IN));
    assertFalse(fromOspf.process(isisL1, ospfIntra.toBuilder(), IN));
  }

  @Test
  public void testStaticRoutePreference() {
    Configuration c = parseConfig("static-route-preference");
    assertThat(
        c,
        hasVrf(
            "default",
            hasStaticRoutes(
                equalTo(
                    ImmutableSet.of(
                        StaticRoute.testBuilder()
                            .setNetwork(Prefix.parse("1.2.3.4/24"))
                            .setNextHopIp(Ip.parse("10.0.0.1"))
                            .setAdministrativeCost(250)
                            .setRecursive(false)
                            .build(),
                        StaticRoute.testBuilder()
                            .setNetwork(Prefix.parse("2.3.4.5/24"))
                            .setNextHopIp(Ip.parse("10.0.0.2"))
                            .setAdministrativeCost(5)
                            .setRecursive(false)
                            .build())))));
  }

  @Test
  public void testStaticRouteOverwrite() {
    JuniperConfiguration c = parseJuniperConfig("static-route-overwrite");
    Map<Prefix, StaticRouteV4> staticRoutes =
        c.getMasterLogicalSystem()
            .getRoutingInstances()
            .get(DEFAULT_VRF_NAME)
            .getRibs()
            .get(RIB_IPV4_UNICAST)
            .getStaticRoutes();
    StaticRouteV4 r0 = staticRoutes.get(Prefix.parse("10.0.0.0/16"));
    StaticRouteV4 r1 = staticRoutes.get(Prefix.parse("10.1.0.0/16"));
    StaticRouteV4 r2 = staticRoutes.get(Prefix.parse("10.2.0.0/16"));
    StaticRouteV4 r3 = staticRoutes.get(Prefix.parse("10.3.0.0/16"));

    // Old next-hops are cleared
    assertFalse(r0.getDrop());
    assertNull(r1.getNextTable());
    assertThat(r2.getNextHopIp(), emptyIterable());
    assertThat(r2.getNextHopInterface(), emptyIterable());
    assertNull(r3.getNextTable());

    // New next-hops are set
    assertThat(r0.getNextHopIp(), contains(Ip.parse("10.0.0.1")));
    assertThat(r1.getNextHopInterface(), contains("ge-0/0/0.0"));
    assertThat(r2.getNextTable(), equalTo("ri2.inet.0"));
    assertTrue(r3.getDrop());
  }

  @Test
  public void testStaticRouteConversion() {
    Configuration c = parseConfig("static-routes");
    assertThat(
        c,
        allOf(
            hasDefaultVrf(
                hasStaticRoutes(
                    containsInAnyOrder(
                        StaticRoute.builder()
                            .setNetwork(Prefix.parse("1.0.0.0/8"))
                            .setNextHopIp(Ip.parse("10.0.0.1"))
                            .setAdministrativeCost(5)
                            .setRecursive(false)
                            .build(),
                        StaticRoute.builder()
                            .setNetwork(Prefix.parse("3.0.0.0/8"))
                            .setNextHop(NextHopDiscard.instance())
                            .setNonForwarding(true)
                            .setRecursive(false)
                            .setAdministrativeCost(5)
                            .build(),
                        StaticRoute.builder()
                            .setNetwork(Prefix.parse("4.0.0.0/8"))
                            .setNextHopInterface("ge-0/0/0.0")
                            .setAdministrativeCost(5)
                            .setRecursive(false)
                            .build(),
                        StaticRoute.builder()
                            .setNetwork(Prefix.parse("4.0.0.0/8"))
                            .setNextHopIp(Ip.parse("10.0.0.1"))
                            .setAdministrativeCost(5)
                            .setRecursive(false)
                            .build(),
                        StaticRoute.builder()
                            .setNetwork(Prefix.parse("4.0.0.0/8"))
                            .setNextHopIp(Ip.parse("10.0.0.2"))
                            .setAdministrativeCost(5)
                            .setRecursive(false)
                            .build(),
                        StaticRoute.builder()
                            .setNetwork(Prefix.parse("6.0.0.0/8"))
                            .setNextHopIp(Ip.parse("10.0.0.1"))
                            .setAdministrativeCost(5)
                            .setRecursive(true)
                            .build(),
                        StaticRoute.builder()
                            .setNetwork(Prefix.parse("7.0.0.0/8"))
                            .setNextHop(NextHopVrf.of("ri2"))
                            .setAdministrativeCost(5)
                            .setRecursive(false)
                            .build(),
                        StaticRoute.builder()
                            .setNetwork(Prefix.parse("10.0.0.0/16"))
                            .setNextHopIp(Ip.parse("1.2.3.4"))
                            .setAdministrativeCost(5)
                            .setRecursive(false)
                            .build(),
                        StaticRoute.builder()
                            .setNetwork(Prefix.parse("10.0.0.0/16"))
                            .setNextHopIp(Ip.parse("1.2.3.5"))
                            .setAdministrativeCost(5)
                            .setRecursive(false)
                            .build(),
                        StaticRoute.builder()
                            .setNetwork(Prefix.parse("11.0.0.0/32"))
                            .setNextHopIp(Ip.parse("1.2.3.6"))
                            .setAdministrativeCost(5)
                            .setRecursive(false)
                            .build(),
                        StaticRoute.builder()
                            .setNetwork(Prefix.parse("12.0.0.0/8"))
                            .setNextHop(NextHopIp.of(Ip.parse("1.2.3.4")))
                            .setAdministrativeCost(5)
                            .setRecursive(false)
                            .setTag(12)
                            .build(),
                        StaticRoute.builder()
                            .setNetwork(Prefix.parse("13.0.0.0/8"))
                            .setNextHop(NextHopIp.of(Ip.parse("1.2.3.4")))
                            .setAdministrativeCost(5)
                            .setRecursive(false)
                            .build()))),
            hasVrf(
                "ri2",
                hasStaticRoutes(
                    contains(
                        StaticRoute.builder()
                            .setNetwork(Prefix.parse("2.0.0.0/8"))
                            .setNextHopIp(Ip.parse("10.0.0.2"))
                            .setAdministrativeCost(5)
                            .setRecursive(false)
                            .build())))));

    assertThat(
        c,
        hasVrf(
            "ri3",
            hasStaticRoutes(
                containsInAnyOrder(
                    // normal next-hop
                    StaticRoute.builder()
                        .setNetwork(Prefix.parse("5.5.5.0/24"))
                        .setNextHopIp(Ip.parse("2.3.4.5"))
                        .setAdministrativeCost(150)
                        .setRecursive(false)
                        .setMetric(6L)
                        .build(),
                    // inherits admin from the static route preference
                    StaticRoute.builder()
                        .setNetwork(Prefix.parse("5.5.5.0/24"))
                        .setNextHopInterface("ge-0/0/0.0")
                        .setAdministrativeCost(150)
                        .setRecursive(false)
                        .setMetric(6L)
                        .build(),
                    // qualified next-hop overrides admin and tag
                    StaticRoute.builder()
                        .setNetwork(Prefix.parse("5.5.5.0/24"))
                        .setNextHopIp(Ip.parse("1.2.3.4"))
                        .setAdministrativeCost(180)
                        .setTag(4294967295L)
                        .setMetric(7L)
                        .setRecursive(false)
                        .build(),
                    StaticRoute.builder()
                        .setNetwork(Prefix.parse("8.0.0.0/8"))
                        .setNextHop(NextHopVrf.of(DEFAULT_VRF_NAME))
                        .setAdministrativeCost(5)
                        .setRecursive(false)
                        .build()))));
  }

  @Test
  public void testStaticRouteParsing() {
    JuniperConfiguration c = parseJuniperConfig("static-routes");
    Map<Prefix, StaticRouteV4> routes =
        c.getMasterLogicalSystem()
            .getRoutingInstances()
            .get("default")
            .getRibs()
            .get(RIB_IPV4_UNICAST)
            .getStaticRoutes();
    Map<Prefix, StaticRouteV4> routes2 =
        c.getMasterLogicalSystem()
            .getRoutingInstances()
            .get("ri2")
            .getRibs()
            .get(RIB_IPV4_UNICAST)
            .getStaticRoutes();
    {
      Prefix p = Prefix.parse("1.0.0.0/8");
      assertThat(routes, hasKey(p));
      assertThat(routes.get(p).getNextHopIp(), containsInAnyOrder(Ip.parse("10.0.0.1")));
      assertThat(routes.get(p).getInstall(), equalTo(Boolean.TRUE));
      assertThat(routes.get(p).getReadvertise(), equalTo(Boolean.TRUE));
    }
    {
      Prefix p = Prefix.parse("2.0.0.0/8");
      assertThat(routes2, hasKey(p));
      assertThat(routes2.get(p).getNextHopIp(), containsInAnyOrder(Ip.parse("10.0.0.2")));
      assertThat(routes2.get(p).getInstall(), nullValue());
      assertThat(routes2.get(p).getReadvertise(), nullValue());
      assertThat(routes2.get(p).getTag(), nullValue());
      assertThat(routes2.get(p).getTag2(), nullValue());
    }
    {
      Prefix p = Prefix.parse("12.0.0.0/8");
      assertThat(routes, hasKey(p));
      assertThat(routes.get(p).getReadvertise(), equalTo(Boolean.FALSE));
      assertThat(routes.get(p).getNextHopIp(), containsInAnyOrder(Ip.parse("1.2.3.4")));
      assertThat(routes.get(p).getTag(), equalTo(12L));
      assertThat(routes.get(p).getTag2(), equalTo(1212L));
    }
  }

  @Test
  public void testStaticRouteIPv6Parsing() {
    JuniperConfiguration c = parseJuniperConfig("static-routes-ipv6");
    RoutingInformationBase routesDefault =
        c.getMasterLogicalSystem()
            .getRoutingInstances()
            .get("default")
            .getRibs()
            .get(RIB_IPV6_UNICAST);
    RoutingInformationBase routesTestVrf =
        c.getMasterLogicalSystem()
            .getRoutingInstances()
            .get("TEST-VRF")
            .getRibs()
            .get("TEST-VRF.inet6.0");
    assertThat(routesDefault.getStaticRoutes().keySet(), empty());
    assertThat(routesTestVrf.getStaticRoutes().keySet(), empty());

    {
      Prefix6 p = Prefix6.parse("01f1:8:1e:8fff::10/57");
      assertThat(routesDefault.getStaticRoutesV6(), hasKey(p));
      StaticRouteV6 route = routesDefault.getStaticRoutesV6().get(p);
      assertThat(route.getPrefix6(), equalTo(p));
      assertThat(route.getTag(), equalTo(12L));
      assertThat(route.getTag2(), equalTo(1212L));
      assertThat(route.getNextHopIp(), containsInAnyOrder(Ip6.parse("21f1:8:1e:8fff::11")));
      NextHop nh = new NextHop("ge-0/0/0.0");
      assertThat(route.getQualifiedNextHops(), hasKey(nh));
      assertThat(
          c.getStructureManager().getStructureReferences(JuniperStructureType.INTERFACE),
          hasKey("ge-0/0/0.0"));
    }
    {
      Prefix6 p = Prefix6.parse("3ff1:8:1e:8fff::10/89");
      assertThat(routesDefault.getStaticRoutesV6(), hasKey(p));
      StaticRouteV6 route = routesDefault.getStaticRoutesV6().get(p);
      NextHop nh = new NextHop(Ip6.parse("3ff1:8:1e:8fff::11"));
      assertThat(route.getQualifiedNextHops(), hasKey(nh));
      assertThat(route.getQualifiedNextHops().get(nh).getPreference(), equalTo(180));
      assertThat(route.getPrefix6(), equalTo(p));
      assertThat(route.getDistance(), equalTo(150));
      assertThat(route.getInstall(), equalTo(Boolean.TRUE));
      assertThat(route.getMetric(), equalTo(6));
      assertThat(route.getResolve(), equalTo(Boolean.TRUE));
    }
    {
      Prefix6 p = Prefix6.parse("0ff1:8:1e:8fff::/42");
      assertThat(routesTestVrf.getStaticRoutesV6(), hasKey(p));
      StaticRouteV6 route = routesTestVrf.getStaticRoutesV6().get(p);
      assertThat(route.getPrefix6(), equalTo(p));
      assertThat(route.getDrop(), equalTo(Boolean.TRUE));
      assertThat(route.getInstall(), equalTo(Boolean.FALSE));
      assertThat(route.getTag(), equalTo(201L));
    }
    {
      // Check default 128 if no prefix length is provided
      Prefix6 p = Prefix6.parse("52f1:1:1e:8f1f::8/128");
      assertThat(routesDefault.getStaticRoutesV6(), hasKey(p));
      StaticRouteV6 route = routesDefault.getStaticRoutesV6().get(p);
      assertThat(route.getPrefix6(), equalTo(p));
    }
    {
      // Simple test of RIB routing-instance name allowing numbers and underscores
      RoutingInformationBase routesTestVrf2 =
          c.getMasterLogicalSystem()
              .getRoutingInstances()
              .get("TEST_VRF2")
              .getRibs()
              .get("TEST_VRF2.inet6.0");
      assertThat(routesTestVrf2.getStaticRoutesV6(), hasKey(Prefix6.parse("0ff1:8:1e:8fff::/42")));
    }
  }

  @Test
  public void testStaticRoutesWarnings() throws IOException {
    String hostname = "static-routes-warn";
    Configuration c = parseConfig(hostname);
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    assertThat(
        ccae.getWarnings().get(hostname).getRedFlagWarnings(),
        containsInAnyOrder(
            WarningMatchers.hasText(
                containsString("cannot contain both discard nexthop and qualified-next-hop")),
            WarningMatchers.hasText(
                containsString("contains both next-table and qualified-next-hop"))));
    // Only the valid route is added
    assertThat(
        c,
        allOf(
            hasDefaultVrf(
                hasStaticRoutes(
                    containsInAnyOrder(
                        StaticRoute.builder()
                            .setNetwork(Prefix.parse("10.0.1.0/24"))
                            .setNextHop(NextHopDiscard.instance())
                            .setNonForwarding(true)
                            .setAdministrativeCost(5)
                            .setRecursive(false)
                            .build())))));
  }

  @Test
  public void testStormControl() {
    /* allow storm-control configuration in an interface */
    parseConfig("storm-control");
  }

  @Test
  public void testSecurityAddressBookGlobalAddress() {
    Configuration config = parseConfig("security-address-book-global-address");
    Map<String, IpSpace> ipSpaces = config.getIpSpaces();
    assertThat(ipSpaces.keySet(), contains("global~NAME"));
  }

  @Test
  public void testSecurityPolicy() {
    JuniperConfiguration juniperConfiguration = parseJuniperConfig("security-policy");
    Map<String, Zone> zones = juniperConfiguration.getMasterLogicalSystem().getZones();

    assertThat(zones.keySet(), containsInAnyOrder("trust", "untrust"));

    Zone trust = zones.get("trust");
    assertThat(trust.getFromZonePolicies().keySet(), hasSize(0));
    assertThat(trust.getToZonePolicies().keySet(), hasSize(1));

    Zone untrust = zones.get("untrust");
    assertThat(untrust.getFromZonePolicies().keySet(), hasSize(1));
    assertThat(untrust.getToZonePolicies().keySet(), hasSize(0));
  }

  @Test
  public void testSnmpClientIps() {
    Configuration c = parseConfig("snmp");
    Map<String, SnmpCommunity> communities = c.getDefaultVrf().getSnmpServer().getCommunities();
    assertThat(communities, hasKeys("COMM1", "COMM2"));
    {
      SnmpCommunity comm = communities.get("COMM1");
      assertThat(
          comm.getClientIps(),
          equalTo(
              AclIpSpace.union(
                  Prefix.parse("1.2.3.4/31").toIpSpace(), Prefix.parse("10.0.0.0/8").toIpSpace())));
    }
    {
      SnmpCommunity comm = communities.get("COMM2");
      assertThat(
          comm.getClientIps(),
          equalTo(
              AclIpSpace.union(
                  Ip.parse("2.3.4.5").toIpSpace(), Prefix.parse("20.0.0.0/8").toIpSpace())));
    }
  }

  @Test
  public void testPreSourceNatOutgoingFilter() {
    Configuration config = parseConfig("security-policy");
    String ifaceIn = "ge-0/0/0.0";
    String ifaceOut = "ge-0/0/1.0";

    IpAccessList securityPolicy1 =
        config.getAllInterfaces().get(ifaceOut).getPreTransformationOutgoingFilter();

    // Any arbitrary flow from trust to untrust should be permitted
    Flow flow1 = createFlow(IpProtocol.UDP, 90);
    Flow flow2 = createFlow(IpProtocol.TCP, 9000);

    assertThat(
        securityPolicy1, accepts(flow1, ifaceIn, config.getIpAccessLists(), config.getIpSpaces()));

    assertThat(
        securityPolicy1, accepts(flow2, ifaceIn, config.getIpAccessLists(), config.getIpSpaces()));

    // Packet to ifaceIn should be denied by default
    IpAccessList securityPolicy2 =
        config.getAllInterfaces().get(ifaceIn).getPreTransformationOutgoingFilter();

    assertThat(
        securityPolicy2, rejects(flow1, ifaceOut, config.getIpAccessLists(), config.getIpSpaces()));

    assertThat(
        securityPolicy2, rejects(flow2, ifaceOut, config.getIpAccessLists(), config.getIpSpaces()));
  }

  @Test
  public void testPsFromInterface() {
    Configuration config = parseConfig("juniper-routing-policy");

    // Matches iface prefix, connected route
    for (Prefix p : ImmutableList.of(Prefix.parse("1.1.1.1/24"), Prefix.parse("2.2.2.2/24"))) {
      Result result =
          config
              .getRoutingPolicies()
              .get("POLICY-NAME")
              .call(
                  Environment.builder(config)
                      .setOriginalRoute(new ConnectedRoute(p, "iface"))
                      .build());
      assertThat(result.getBooleanValue(), equalTo(true));
    }

    // Does not match wrong prefix
    Result result =
        config
            .getRoutingPolicies()
            .get("POLICY-NAME")
            .call(
                Environment.builder(config)
                    .setOriginalRoute(new ConnectedRoute(Prefix.parse("3.3.3.3/24"), "iface"))
                    .build());
    assertThat(result.getBooleanValue(), equalTo(false));

    // Does not match static route, even with correct prefix
    result =
        config
            .getRoutingPolicies()
            .get("POLICY-NAME")
            .call(
                Environment.builder(config)
                    .setOriginalRoute(
                        StaticRoute.testBuilder()
                            .setNextHopInterface("iface")
                            .setNetwork(Prefix.parse("1.1.1.1/24"))
                            .setAdministrativeCost(1)
                            .build())
                    .build());
    assertThat(result.getBooleanValue(), equalTo(false));
  }

  @Test
  public void testSourceNatPool() {
    JuniperConfiguration juniperConfiguration = parseJuniperConfig("juniper-sourcenat-pool");
    Map<String, NatPool> pools =
        juniperConfiguration.getMasterLogicalSystem().getNatSource().getPools();

    Ip ip0 = Ip.parse("1.0.0.0");
    Ip ip1 = Ip.parse("1.0.0.1");
    Ip ip2 = Ip.parse("1.0.0.2");
    Ip ip3 = Ip.parse("1.0.0.3");

    assertThat(pools.keySet(), hasSize(5));

    assertThat(pools.get("POOL1").getFromAddress(), equalTo(ip1));
    assertThat(pools.get("POOL1").getToAddress(), equalTo(ip3));

    assertThat(pools.get("POOL2").getFromAddress(), equalTo(ip1));
    assertThat(pools.get("POOL2").getToAddress(), equalTo(ip3));

    assertThat(pools.get("POOL3").getFromAddress(), equalTo(ip0));
    assertThat(pools.get("POOL3").getToAddress(), equalTo(ip0));

    assertThat(pools.get("POOL4").getFromAddress(), equalTo(ip0));
    assertThat(pools.get("POOL4").getToAddress(), equalTo(ip1));

    assertThat(pools.get("POOL5").getFromAddress(), equalTo(ip1));
    assertThat(pools.get("POOL5").getToAddress(), equalTo(ip2));
  }

  @Test
  public void testSrlgExtraction() {
    JuniperConfiguration jc = parseJuniperConfig("junos-srlg");
    assertThat(jc.getMasterLogicalSystem().getSrlgs(), hasKeys("srlg-a", "srlg-b"));
    Srlg srlgA = jc.getMasterLogicalSystem().getSrlgs().get("srlg-a");
    assertThat(srlgA.getCost(), equalTo(10));
    assertThat(srlgA.getValue(), equalTo(101L));
    Srlg srlgB = jc.getMasterLogicalSystem().getSrlgs().get("srlg-b");
    assertThat(srlgB.getCost(), nullValue());
    assertThat(srlgB.getValue(), equalTo(102L));
  }

  @Test
  public void testSrlgReferences() throws IOException {
    String hostname = "junos-srlg";
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    batfish.loadConfigurations(batfish.getSnapshot());
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    assertThat(
        ccae, hasDefinedStructureWithDefinitionLines(filename, SRLG, "srlg-a", contains(4, 5)));
    assertThat(ccae, hasReferencedStructure(filename, SRLG, "srlg-a", MPLS_INTERFACE_SRLG));
  }

  @Test
  public void testScreenOptions() {
    JuniperConfiguration juniperConfiguration = parseJuniperConfig("screen-options");

    assertThat(
        juniperConfiguration.getMasterLogicalSystem().getScreens().get("ALARM_OPTION").getAction(),
        equalTo(ScreenAction.ALARM_WITHOUT_DROP));

    Screen ids = juniperConfiguration.getMasterLogicalSystem().getScreens().get("IDS_OPTION_NAME");

    assertThat(ids, not(nullValue()));

    assertThat(ids.getAction(), equalTo(ScreenAction.DROP));
    assertThat(
        ids.getScreenOptions(),
        equalTo(
            ImmutableList.of(
                IcmpLarge.INSTANCE,
                IpUnknownProtocol.INSTANCE,
                TcpFinNoAck.INSTANCE,
                TcpSynFin.INSTANCE,
                TcpNoFlag.INSTANCE)));
  }

  @Test
  public void testScreenOptionsToVIModel() {
    Configuration config = parseConfig("screen-options");

    IpAccessList inAcl = config.getIpAccessLists().get("FILTER1");
    IpAccessList screenAcl = config.getIpAccessLists().get("~SCREEN~IDS_OPTION_NAME");
    IpAccessList ifaceScreenAcl = config.getIpAccessLists().get("~SCREEN_INTERFACE~ge-0/0/0.0");
    IpAccessList zoneScreenAcl = config.getIpAccessLists().get("~SCREEN_ZONE~untrust");
    IpAccessList combinedInAcl =
        config.getIpAccessLists().get("~COMBINED_INCOMING_FILTER~ge-0/0/0.0");

    assertThat(inAcl, notNullValue());
    assertThat(screenAcl, notNullValue());
    assertThat(zoneScreenAcl, notNullValue());
    assertThat(ifaceScreenAcl, notNullValue());
    assertThat(combinedInAcl, notNullValue());

    List<ScreenOption> supportedOptions =
        ImmutableList.of(
            IcmpLarge.INSTANCE,
            IpUnknownProtocol.INSTANCE,
            TcpFinNoAck.INSTANCE,
            TcpSynFin.INSTANCE,
            TcpNoFlag.INSTANCE);

    assertThat(
        inAcl,
        equalTo(
            IpAccessList.builder()
                .setName("FILTER1")
                .setLines(
                    ImmutableList.of(
                        new ExprAclLine(
                            LineAction.PERMIT,
                            matchSrc(
                                IpWildcard.parse("1.2.3.6").toIpSpace(),
                                TraceElement.of("Matched source-address 1.2.3.6")),
                            "TERM",
                            matchingFirewallFilterTerm(
                                "configs/" + config.getHostname(), "FILTER1", "TERM"),
                            firewallFilterTermVendorStructureId(
                                "configs/" + config.getHostname(), "FILTER1", "TERM"))))
                .build()));

    assertThat(
        screenAcl,
        equalTo(
            IpAccessList.builder()
                .setName("~SCREEN~IDS_OPTION_NAME")
                .setLines(
                    ImmutableList.of(
                        ExprAclLine.rejecting(
                            new OrMatchExpr(
                                supportedOptions.stream()
                                    .map(ScreenOption::getAclLineMatchExpr)
                                    .collect(Collectors.toList()))),
                        ExprAclLine.ACCEPT_ALL))
                .build()));

    assertThat(
        zoneScreenAcl,
        equalTo(
            IpAccessList.builder()
                .setName("~SCREEN_ZONE~untrust")
                .setLines(
                    ImmutableList.of(
                        ExprAclLine.accepting(new PermittedByAcl("~SCREEN~IDS_OPTION_NAME"))))
                .build()));

    assertThat(
        ifaceScreenAcl,
        equalTo(
            IpAccessList.builder()
                .setName("~SCREEN_INTERFACE~ge-0/0/0.0")
                .setLines(
                    ImmutableList.of(
                        ExprAclLine.accepting(new PermittedByAcl("~SCREEN_ZONE~untrust"))))
                .build()));

    assertThat(
        combinedInAcl,
        equalTo(
            IpAccessList.builder()
                .setName("~COMBINED_INCOMING_FILTER~ge-0/0/0.0")
                .setLines(
                    ImmutableList.of(
                        ExprAclLine.accepting(
                            new AndMatchExpr(
                                ImmutableList.of(
                                    new PermittedByAcl("~SCREEN_INTERFACE~ge-0/0/0.0"),
                                    new PermittedByAcl("FILTER1"))))))
                .build()));

    assertThat(
        config.getAllInterfaces().get("ge-0/0/0.0").getIncomingFilter(), equalTo(combinedInAcl));
  }

  @Test
  public void testInstanceImport() throws IOException {
    String hostname = "instance-import";
    Configuration c = parseConfig(hostname);

    /*
     * instance-import for default VRF imports two policies, PS1 followed by PS2.
     * PS1 accepts VRF3, then VRF1. PS2 accepts VRF1 and an undefined MYSTERY_VRF, and rejects VRF2.
     * Resulting list of VRFs whose routes to import should have the referenced VRFs in the order
     * they were referenced, ignoring second reference to VRF1, not including undefined MYSTERY_VRF.
     */
    Vrf defaultVrf = c.getVrfs().get(DEFAULT_VRF_NAME);
    assertNotNull(defaultVrf.getVrfLeakConfig());
    MainRibVrfLeakConfig.Builder leakConfigBuilder =
        MainRibVrfLeakConfig.builder()
            .setImportPolicy(generateInstanceImportPolicyName(DEFAULT_VRF_NAME));
    assertThat(
        defaultVrf.getVrfLeakConfig().getMainRibVrfLeakConfigs(),
        containsInAnyOrder(
            leakConfigBuilder.setImportFromVrf("VRF3").build(),
            leakConfigBuilder.setImportFromVrf("VRF1").build(),
            leakConfigBuilder.setImportFromVrf("VRF2").build()));

    /*
    Test instance import policy behavior.
     - Routes from VRF1 and VRF3 should be accepted, but not routes from VRF2.
     - Routes from arbitrary other VRFs should be rejected by default policy.
    The only thing in the arguments to process that should get checked is source VRF.
    */
    RoutingPolicy instanceImportPolicy =
        c.getRoutingPolicies().get(generateInstanceImportPolicyName(DEFAULT_VRF_NAME));
    StaticRoute sr = StaticRoute.testBuilder().setNetwork(Prefix.ZERO).setAdmin(5).build();
    assertThat(
        instanceImportPolicy.process(new AnnotatedRoute<>(sr, "VRF1"), null, null), equalTo(true));
    assertThat(
        instanceImportPolicy.process(new AnnotatedRoute<>(sr, "VRF2"), null, null), equalTo(false));
    assertThat(
        instanceImportPolicy.process(new AnnotatedRoute<>(sr, "VRF3"), null, null), equalTo(true));
    assertThat(
        instanceImportPolicy.process(new AnnotatedRoute<>(sr, "ANOTHER_VRF"), null, null),
        equalTo(false));

    /*
    Check resulting state of routes in snapshot.
     - VRF1 has static 1.1.1.1/30, which should be in default VRF since VRF1 routes are accepted
     - VRF2 has static 2.2.2.2/30, which should not be in default VRF since VRF2 routes are rejected
     - VRF3 doesn't have any routes, so should have no impact on default VRF
    */
    Batfish batfish = BatfishTestUtils.getBatfish(ImmutableSortedMap.of(hostname, c), _folder);
    batfish.computeDataPlane(batfish.getSnapshot());
    IncrementalDataPlane dp = (IncrementalDataPlane) batfish.loadDataPlane(batfish.getSnapshot());
    ImmutableMap<String, Set<AnnotatedRoute<AbstractRoute>>> routes =
        dp.getRibsForTesting().get(hostname).entrySet().stream()
            .collect(ImmutableMap.toImmutableMap(Entry::getKey, e -> e.getValue().getRoutes()));

    assertThat(
        routes.get(DEFAULT_VRF_NAME),
        contains(allOf(hasPrefix(Prefix.parse("1.1.1.1/30")), hasSourceVrf("VRF1"))));

    // Ensure that VRF2 does in fact have 2.2.2.2/30, as expected
    assertThat(routes.get("VRF2"), hasItem(hasPrefix(Prefix.parse("2.2.2.2/30"))));
  }

  @Test
  public void testInterfaceRibGroup() throws IOException {
    String hostname = "juniper-interface-ribgroup";
    Configuration c = parseConfig(hostname);
    Batfish batfish = BatfishTestUtils.getBatfish(ImmutableSortedMap.of(hostname, c), _folder);
    batfish.computeDataPlane(batfish.getSnapshot());
    IncrementalDataPlane dp = (IncrementalDataPlane) batfish.loadDataPlane(batfish.getSnapshot());

    ImmutableMap<String, Set<AnnotatedRoute<AbstractRoute>>> routes =
        dp.getRibsForTesting().get(hostname).entrySet().stream()
            .collect(ImmutableMap.toImmutableMap(Entry::getKey, e -> e.getValue().getRoutes()));
    String vrf2Name = "VRF2";

    Set<AnnotatedRoute<AbstractRoute>> defaultExpectedRoutes =
        ImmutableSet.of(
            new AnnotatedRoute<>(
                new ConnectedRoute(Prefix.parse("1.1.1.1/32"), "lo0.0"), DEFAULT_VRF_NAME),
            new AnnotatedRoute<>(
                new ConnectedRoute(Prefix.parse("2.2.2.2/31"), "ge-0/0/0.0"), DEFAULT_VRF_NAME),
            new AnnotatedRoute<>(
                new LocalRoute(ConcreteInterfaceAddress.parse("2.2.2.2/31"), "ge-0/0/0.0"),
                DEFAULT_VRF_NAME));
    Set<AnnotatedRoute<AbstractRoute>> vrf2ExpectedRoutes =
        ImmutableSet.of(
            // From default VRF
            new AnnotatedRoute<>(
                new ConnectedRoute(Prefix.parse("1.1.1.1/32"), "lo0.0"), DEFAULT_VRF_NAME),
            new AnnotatedRoute<>(
                new ConnectedRoute(Prefix.parse("2.2.2.2/31"), "ge-0/0/0.0"), DEFAULT_VRF_NAME),
            new AnnotatedRoute<>(
                new LocalRoute(ConcreteInterfaceAddress.parse("2.2.2.2/31"), "ge-0/0/0.0"),
                DEFAULT_VRF_NAME),
            // Present normally
            new AnnotatedRoute<>(
                new ConnectedRoute(Prefix.parse("2.2.2.8/31"), "ge-0/0/3.0"), vrf2Name),
            new AnnotatedRoute<>(
                new LocalRoute(ConcreteInterfaceAddress.parse("2.2.2.8/31"), "ge-0/0/3.0"),
                vrf2Name));
    assertThat(routes.get(DEFAULT_VRF_NAME), equalTo(defaultExpectedRoutes));
    assertThat(routes.get(vrf2Name), equalTo(vrf2ExpectedRoutes));
  }

  @Test
  public void testInterfaceRibGroupWithPolicies() throws IOException {
    String hostname = "juniper-interface-ribgroup-with-policy";
    Configuration c = parseConfig(hostname);
    Batfish batfish = BatfishTestUtils.getBatfish(ImmutableSortedMap.of(hostname, c), _folder);
    batfish.computeDataPlane(batfish.getSnapshot());
    IncrementalDataPlane dp = (IncrementalDataPlane) batfish.loadDataPlane(batfish.getSnapshot());

    ImmutableMap<String, Set<AnnotatedRoute<AbstractRoute>>> routes =
        dp.getRibsForTesting().get(hostname).entrySet().stream()
            .collect(ImmutableMap.toImmutableMap(Entry::getKey, e -> e.getValue().getRoutes()));
    String vrf2Name = "VRF2";

    Set<AnnotatedRoute<AbstractRoute>> vrf2ExpectedRoutes =
        ImmutableSet.of(
            // allowed Default policy
            new AnnotatedRoute<>(
                new ConnectedRoute(Prefix.parse("1.1.1.1/32"), "lo0.0"), DEFAULT_VRF_NAME),
            new AnnotatedRoute<>(
                new LocalRoute(ConcreteInterfaceAddress.parse("2.2.2.2/31"), "ge-0/0/0.0"),
                DEFAULT_VRF_NAME),
            new AnnotatedRoute<>(
                new LocalRoute(ConcreteInterfaceAddress.parse("2.2.2.4/31"), "ge-0/0/1.0"),
                DEFAULT_VRF_NAME),
            new AnnotatedRoute<>(
                new LocalRoute(ConcreteInterfaceAddress.parse("2.2.2.6/31"), "ge-0/0/2.0"),
                DEFAULT_VRF_NAME),
            // allowed by RIB_IN
            new AnnotatedRoute<>(
                new ConnectedRoute(Prefix.parse("2.2.2.2/31"), "ge-0/0/0.0"), DEFAULT_VRF_NAME),
            new AnnotatedRoute<>(
                new ConnectedRoute(Prefix.parse("2.2.2.4/31"), "ge-0/0/1.0"), DEFAULT_VRF_NAME),
            // Present normally
            new AnnotatedRoute<>(
                new ConnectedRoute(Prefix.parse("2.2.2.8/31"), "ge-0/0/3.0"), vrf2Name),
            new AnnotatedRoute<>(
                new LocalRoute(ConcreteInterfaceAddress.parse("2.2.2.8/31"), "ge-0/0/3.0"),
                vrf2Name));
    Set<AnnotatedRoute<AbstractRoute>> defaultExpectedRoutes =
        ImmutableSet.of(
            new AnnotatedRoute<>(
                new ConnectedRoute(Prefix.parse("1.1.1.1/32"), "lo0.0"), DEFAULT_VRF_NAME),
            new AnnotatedRoute<>(
                new ConnectedRoute(Prefix.parse("2.2.2.2/31"), "ge-0/0/0.0"), DEFAULT_VRF_NAME),
            new AnnotatedRoute<>(
                new LocalRoute(ConcreteInterfaceAddress.parse("2.2.2.2/31"), "ge-0/0/0.0"),
                DEFAULT_VRF_NAME),
            new AnnotatedRoute<>(
                new ConnectedRoute(Prefix.parse("2.2.2.4/31"), "ge-0/0/1.0"), DEFAULT_VRF_NAME),
            new AnnotatedRoute<>(
                new LocalRoute(ConcreteInterfaceAddress.parse("2.2.2.4/31"), "ge-0/0/1.0"),
                DEFAULT_VRF_NAME),
            new AnnotatedRoute<>(
                new ConnectedRoute(Prefix.parse("2.2.2.6/31"), "ge-0/0/2.0"), DEFAULT_VRF_NAME),
            new AnnotatedRoute<>(
                new LocalRoute(ConcreteInterfaceAddress.parse("2.2.2.6/31"), "ge-0/0/2.0"),
                DEFAULT_VRF_NAME));
    assertThat(routes.get(DEFAULT_VRF_NAME), equalTo(defaultExpectedRoutes));
    assertThat(routes.get(vrf2Name), equalTo(vrf2ExpectedRoutes));
  }

  @Test
  public void testInterfaceRibGroupWithTransformation() throws IOException {
    String hostname = "juniper-interface-ribgroup-with-transformation";
    Configuration c = parseConfig(hostname);
    Batfish batfish = BatfishTestUtils.getBatfish(ImmutableSortedMap.of(hostname, c), _folder);
    batfish.computeDataPlane(batfish.getSnapshot());
    IncrementalDataPlane dp = (IncrementalDataPlane) batfish.loadDataPlane(batfish.getSnapshot());

    ImmutableMap<String, Set<AnnotatedRoute<AbstractRoute>>> routes =
        dp.getRibsForTesting().get(hostname).entrySet().stream()
            .collect(ImmutableMap.toImmutableMap(Entry::getKey, e -> e.getValue().getRoutes()));
    String vrf2Name = "VRF2";

    Set<AnnotatedRoute<AbstractRoute>> defaultExpectedRoutes =
        ImmutableSet.of(
            new AnnotatedRoute<>(
                new ConnectedRoute(Prefix.parse("1.1.1.1/32"), "lo0.0"), DEFAULT_VRF_NAME),
            new AnnotatedRoute<>(
                new ConnectedRoute(Prefix.parse("2.2.2.2/31"), "ge-0/0/0.0", 0), DEFAULT_VRF_NAME),
            new AnnotatedRoute<>(
                new LocalRoute(ConcreteInterfaceAddress.parse("2.2.2.2/31"), "ge-0/0/0.0"),
                DEFAULT_VRF_NAME));
    Set<AnnotatedRoute<AbstractRoute>> vrf2ExpectedRoutes =
        ImmutableSet.of(
            new AnnotatedRoute<>(
                new ConnectedRoute(Prefix.parse("1.1.1.1/32"), "lo0.0"), DEFAULT_VRF_NAME),
            new AnnotatedRoute<>(
                new LocalRoute(ConcreteInterfaceAddress.parse("2.2.2.2/31"), "ge-0/0/0.0"),
                DEFAULT_VRF_NAME),
            new AnnotatedRoute<>(
                new ConnectedRoute(Prefix.parse("2.2.2.2/31"), "ge-0/0/0.0", 123),
                DEFAULT_VRF_NAME));
    assertThat(routes.get(DEFAULT_VRF_NAME), equalTo(defaultExpectedRoutes));
    assertThat(routes.get(vrf2Name), equalTo(vrf2ExpectedRoutes));
  }

  @Test
  public void testBgpRibGroup() throws IOException {
    String hostname = "juniper-bgp-rib-group";
    Configuration c = parseConfig(hostname);
    BatfishTestUtils.getBatfish(ImmutableSortedMap.of(hostname, c), _folder);

    assertThat(
        c.getDefaultVrf()
            .getBgpProcess()
            .getActiveNeighbors()
            .get(Ip.parse("1.1.1.3"))
            .getAppliedRibGroup()
            .getName(),
        equalTo("RIB_GROUP_1"));
    assertThat(
        c.getDefaultVrf()
            .getBgpProcess()
            .getActiveNeighbors()
            .get(Ip.parse("1.1.1.5"))
            .getAppliedRibGroup()
            .getName(),
        equalTo("RIB_GROUP_2"));
  }

  /** Throws the creation of {@link FirewallSessionInterfaceInfo} objects for juniper devices. */
  @Test
  public void testFirewallSession() {
    Configuration c = parseConfig("firewall-session-info");

    String i0Name = "ge-0/0/0.0";
    String i1Name = "ge-0/0/0.1";
    String i2Name = "ge-0/0/0.2";
    String i3Name = "ge-0/0/0.3";

    assertThat(
        c.getAllInterfaces().get(i0Name).getFirewallSessionInterfaceInfo(),
        equalTo(
            new FirewallSessionInterfaceInfo(
                Action.FORWARD_OUT_IFACE, ImmutableList.of(i0Name, i1Name), "FILTER1", "FILTER2")));

    assertThat(
        c.getAllInterfaces().get(i1Name).getFirewallSessionInterfaceInfo(),
        equalTo(
            new FirewallSessionInterfaceInfo(
                Action.FORWARD_OUT_IFACE, ImmutableList.of(i0Name, i1Name), null, null)));

    assertThat(
        c.getAllInterfaces().get(i2Name).getFirewallSessionInterfaceInfo(),
        equalTo(
            new FirewallSessionInterfaceInfo(
                Action.FORWARD_OUT_IFACE, ImmutableList.of(i2Name), null, null)));

    // ge-0/0/0.3 is not part of any zoone, so no firewall session interface info.
    assertThat(c.getAllInterfaces().get(i3Name).getFirewallSessionInterfaceInfo(), nullValue());
  }

  @Test
  public void testPortAddressTranslation() {
    JuniperConfiguration juniperConfiguration = parseJuniperConfig("juniper-nat-pat");
    Nat sourceNat = juniperConfiguration.getMasterLogicalSystem().getNatSource();
    NatPool pool0 = sourceNat.getPools().get("POOL0");
    NatPool pool1 = sourceNat.getPools().get("POOL1");
    NatPool pool2 = sourceNat.getPools().get("POOL2");
    NatPool pool3 = sourceNat.getPools().get("POOL3");

    // pool0 should not have pat specified
    assertNotNull(pool0);
    assertNull(pool0.getPortAddressTranslation());

    // pool1 should has a pat pool with range [2000,3000] with no port translation configured
    assertNotNull(pool1);
    assertNotNull(pool1.getPortAddressTranslation());
    assertThat(pool1.getPortAddressTranslation(), equalTo(new PatPool(2000, 3000)));

    // pool2 should specify no translation
    assertNotNull(pool2);
    assertNotNull(pool2.getPortAddressTranslation());
    assertThat(pool2.getPortAddressTranslation(), equalTo(NoPortTranslation.INSTANCE));

    // pool3 should have a pat pool ranging [10000,20000]
    assertNotNull(pool3);
    assertNotNull(pool3.getPortAddressTranslation());
    assertThat(pool3.getPortAddressTranslation(), equalTo(new PatPool(10000, 20000)));

    Nat destNat = juniperConfiguration.getMasterLogicalSystem().getNatDestination();
    Map<String, NatPool> pools = destNat.getPools();
    assertThat(pools, hasKeys("POOL4", "POOL5", "POOL6"));

    // pool4 should have a pat pool ranging [6000,6000]
    NatPool pool4 = pools.get("POOL4");
    assertThat(
        pool4.getFromAddress(), allOf(equalTo(pool4.getToAddress()), equalTo(Ip.parse("1.0.0.1"))));
    assertThat(pool4.getPortAddressTranslation(), equalTo(new PatPool(6000, 6000)));

    // pool5 should not have pat specified
    NatPool pool5 = pools.get("POOL5");
    assertNull(pool5.getPortAddressTranslation());

    // pool6 should have a pat pool ranging [6666,6666]
    NatPool pool6 = pools.get("POOL6");
    assertThat(
        pool6.getFromAddress(), allOf(equalTo(pool6.getToAddress()), equalTo(Ip.parse("1.0.0.1"))));
    assertThat(pool6.getPortAddressTranslation(), equalTo(new PatPool(6666, 6666)));
  }

  @Test
  public void testVrfSpecificPoolPortAddressTranslation() {
    JuniperConfiguration juniperConfiguration = parseJuniperConfig("juniper-nat-vrf");
    Nat sourceNat = juniperConfiguration.getMasterLogicalSystem().getNatSource();
    NatPool pool0 = sourceNat.getPools().get("POOL0");
    NatPool pool1 = sourceNat.getPools().get("POOL1");
    assertThat(pool0.getOwner(), equalTo("R1"));
    assertNull(pool1.getOwner());
  }

  @Test
  public void testJuniperNtp() {
    // Parse with no warnings
    parseJuniperConfig("juniper-ntp");
  }

  @Test
  public void testJuniperSyslog() {
    // Parse with no warnings
    parseJuniperConfig("juniper-syslog");
  }

  @Test
  public void testSwitchOptionsVtepSourceInterfaceExtraction() {
    JuniperConfiguration juniperConfiguration = parseJuniperConfig("juniper-vtep-source");
    String vtep =
        juniperConfiguration.getMasterLogicalSystem().getSwitchOptions().getVtepSourceInterface();
    assertEquals("lo0.0", vtep);
  }

  @Test
  public void testSwitchOptionsRouteDistinguisherIpExtraction() {
    JuniperConfiguration juniperConfiguration = parseJuniperConfig("switch-options-rd-ip");
    RouteDistinguisher rd =
        juniperConfiguration.getMasterLogicalSystem().getSwitchOptions().getRouteDistinguisher();
    Ip ip = Ip.parse("14.14.14.14");
    Integer asn1 = 7999;
    assertEquals(RouteDistinguisher.from(ip, asn1), rd);
  }

  @Test
  public void testSwitchOptionsRouteDistinguisherTwoByteAsnExtraction() {
    JuniperConfiguration juniperConfiguration = parseJuniperConfig("switch-options-rd-asn2");
    RouteDistinguisher rd =
        juniperConfiguration.getMasterLogicalSystem().getSwitchOptions().getRouteDistinguisher();
    Integer asn1 = 555;
    long asn2 = 1651000;
    assertEquals(RouteDistinguisher.from(asn1, asn2), rd);
  }

  @Test
  public void testSwitchOptionsRouteDistinguisherFourByteAsnExtraction() {
    JuniperConfiguration juniperConfiguration = parseJuniperConfig("switch-options-rd-asn4");
    RouteDistinguisher rd =
        juniperConfiguration.getMasterLogicalSystem().getSwitchOptions().getRouteDistinguisher();
    long asn1 = 77765000;
    Integer asn2 = 1000;
    assertEquals(RouteDistinguisher.from(asn1, asn2), rd);
  }

  @Test
  public void testStaticNat() {
    JuniperConfiguration juniperConfiguration = parseJuniperConfig("juniper-nat-static");
    Nat nat = juniperConfiguration.getMasterLogicalSystem().getNatStatic();
    assertThat(nat.getRuleSets().keySet(), contains("RULESET1"));

    NatRuleSet ruleset = nat.getRuleSets().get("RULESET1");
    assertThat(ruleset.getRules(), hasSize(2));

    NatRule rule1 = ruleset.getRules().get(0);
    assertThat(rule1.getName(), equalTo("RULE1"));
    assertThat(rule1.getMatches(), contains(new NatRuleMatchDstAddrName("DESTNAME")));
    assertThat(
        rule1.getThen(), equalTo(new NatRuleThenPrefixName("PREFIXNAME", IpField.DESTINATION)));

    NatRule rule2 = ruleset.getRules().get(1);
    assertThat(rule2.getName(), equalTo("RULE2"));
    assertThat(rule2.getMatches(), contains(new NatRuleMatchDstAddr(Prefix.parse("2.0.0.0/24"))));
    assertThat(
        rule2.getThen(),
        equalTo(new NatRuleThenPrefix(Prefix.parse("10.10.10.0/24"), IpField.DESTINATION)));
  }

  @Test
  public void testStaticNatViModel() {
    Configuration config = parseConfig("juniper-nat-static");

    // incoming transformation
    Transformation destNatTransformation =
        when(match(HeaderSpace.builder().setSrcIps(Prefix.parse("3.3.3.3/24").toIpSpace()).build()))
            .apply(new Noop(DEST_NAT))
            .build();

    Transformation rule2IncomingTransformation =
        when(match(HeaderSpace.builder().setDstIps(Prefix.parse("2.0.0.0/24").toIpSpace()).build()))
            .apply(
                new ShiftIpAddressIntoSubnet(
                    STATIC_NAT, DESTINATION, Prefix.parse("10.10.10.0/24")))
            .setOrElse(destNatTransformation)
            .build();

    Transformation expectedIncomingTransformation =
        when(match(
                HeaderSpace.builder().setDstIps(new IpSpaceReference("global~DESTNAME")).build()))
            .apply(
                new ShiftIpAddressIntoSubnet(
                    STATIC_NAT, DESTINATION, Prefix.parse("10.10.10.0/24")))
            .setOrElse(rule2IncomingTransformation)
            .build();

    Transformation incomingTransformation =
        config.getAllInterfaces().get("ge-0/0/0.0").getIncomingTransformation();

    assertThat(incomingTransformation, equalTo(expectedIncomingTransformation));

    // outgoing transformation
    AclLineMatchExpr matchFromLocation = AclLineMatchExprs.matchSrcInterface("ge-0/0/1.0");
    Transformation srcNatTransformation =
        when(matchFromLocation)
            .setAndThen(
                when(match(
                        HeaderSpace.builder()
                            .setSrcIps(Prefix.parse("3.3.3.3/24").toIpSpace())
                            .build()))
                    .apply(new Noop(SOURCE_NAT))
                    .build())
            .build();

    Transformation rule2OutgoingTransformation =
        when(match(
                HeaderSpace.builder().setSrcIps(Prefix.parse("10.10.10.0/24").toIpSpace()).build()))
            .apply(new ShiftIpAddressIntoSubnet(STATIC_NAT, SOURCE, Prefix.parse("2.0.0.0/24")))
            .setOrElse(srcNatTransformation)
            .build();

    Transformation expectedOutgoingTransformation =
        when(match(
                HeaderSpace.builder().setSrcIps(new IpSpaceReference("global~PREFIXNAME")).build()))
            .apply(new ShiftIpAddressIntoSubnet(STATIC_NAT, SOURCE, Prefix.parse("1.0.0.0/24")))
            .setOrElse(rule2OutgoingTransformation)
            .build();

    Transformation outgoingTransformation =
        config.getAllInterfaces().get("ge-0/0/0.0").getOutgoingTransformation();

    assertThat(outgoingTransformation, equalTo(expectedOutgoingTransformation));
  }

  @Test
  public void testOspfAreaInterfaceNeighborExtraction() {
    JuniperConfiguration juniperConfiguration = parseJuniperConfig("ospf-area-interface-neighbor");
    assertNotNull(juniperConfiguration);
    assertNotNull(juniperConfiguration.getMasterLogicalSystem().getInterfaces().get("ge-0/0/0"));
    assertNotNull(
        juniperConfiguration
            .getMasterLogicalSystem()
            .getInterfaces()
            .get("ge-0/0/0")
            .getUnits()
            .get("ge-0/0/0.0"));
    assertThat(
        juniperConfiguration
            .getMasterLogicalSystem()
            .getInterfaces()
            .get("ge-0/0/0")
            .getUnits()
            .get("ge-0/0/0.0")
            .getEffectiveOspfSettings()
            .getOspfNeighbors(),
        contains(new InterfaceOspfNeighbor(Ip.parse("1.0.0.1"))));

    assertNotNull(
        juniperConfiguration
            .getMasterLogicalSystem()
            .getInterfaces()
            .get("ge-0/0/1")
            .getUnits()
            .get("ge-0/0/1.0"));
    InterfaceOspfNeighbor neighbor = new InterfaceOspfNeighbor(Ip.parse("2.0.0.1"));
    neighbor.setDesignated(true);
    assertThat(
        juniperConfiguration
            .getMasterLogicalSystem()
            .getInterfaces()
            .get("ge-0/0/1")
            .getUnits()
            .get("ge-0/0/1.0")
            .getEffectiveOspfSettings()
            .getOspfNeighbors(),
        contains(neighbor));
  }

  @Test
  public void testOspfAreaInterfaceNeighborConversion() {
    Configuration config = parseConfig("ospf-area-interface-neighbor");

    assertNotNull(config.getAllInterfaces().get("ge-0/0/0.0").getOspfSettings().getNbmaNeighbors());
    assertThat(
        config.getAllInterfaces().get("ge-0/0/0.0").getOspfSettings().getNbmaNeighbors(),
        contains(Ip.parse("1.0.0.1")));

    assertThat(
        config.getAllInterfaces().get("ge-0/0/1.0").getOspfSettings().getNbmaNeighbors(), empty());
  }

  @Test
  public void testRethExtraction() {
    JuniperConfiguration config = parseJuniperConfig("juniper-reth");

    assertThat(
        config.getMasterLogicalSystem().getInterfaces(),
        hasKeys("reth1", "reth2", "ge-1/0/0", "ge-1/0/1"));
    {
      org.batfish.representation.juniper.Interface iface =
          config.getMasterLogicalSystem().getInterfaces().get("reth1");
      assertThat(iface.getUnits(), hasKeys("reth1.1"));
    }
    {
      org.batfish.representation.juniper.Interface iface =
          config.getMasterLogicalSystem().getInterfaces().get("reth2");
      assertThat(iface.getUnits(), hasKeys("reth2.1"));
    }
    {
      org.batfish.representation.juniper.Interface iface =
          config.getMasterLogicalSystem().getInterfaces().get("ge-1/0/0");
      assertThat(iface.getRedundantParentInterface(), equalTo("reth1"));
    }
    {
      org.batfish.representation.juniper.Interface iface =
          config.getMasterLogicalSystem().getInterfaces().get("ge-1/0/1");
      assertThat(iface.getRedundantParentInterface(), equalTo("reth1"));
    }
  }

  @Test
  public void testRethConversion() {
    Configuration config = parseConfig("juniper-reth");

    assertThat(
        config.getAllInterfaces(),
        hasKeys("reth1", "reth1.1", "reth2", "reth2.1", "ge-1/0/0", "ge-1/0/1"));
    {
      Interface iface = config.getAllInterfaces().get("reth1");
      assertThat(
          iface.getDependencies(),
          containsInAnyOrder(
              new Dependency("ge-1/0/0", DependencyType.AGGREGATE),
              new Dependency("ge-1/0/1", DependencyType.AGGREGATE)));
      assertThat(iface.getBandwidth(), equalTo(1E9D));
      assertTrue(iface.getActive());
    }
    {
      Interface iface = config.getAllInterfaces().get("reth1.1");
      assertThat(iface.getBandwidth(), equalTo(1E9D));
      assertTrue(iface.getActive());
    }
    {
      Interface iface = config.getAllInterfaces().get("reth2");
      assertThat(iface.getDependencies(), empty());
      assertThat(iface.getBandwidth(), equalTo(0.0D));
      assertFalse(iface.getActive());
    }
    {
      Interface iface = config.getAllInterfaces().get("reth2.1");
      assertThat(iface.getBandwidth(), equalTo(0.0D));
      assertFalse(iface.getActive());
    }
  }

  @Test
  public void testVlanAccessExtraction() {
    JuniperConfiguration vc = parseJuniperConfig("juniper-vlan-access");

    assertThat(
        vc.getMasterLogicalSystem().getInterfaces(),
        hasKeys("et-0/0/2", "et-0/0/3", "et-0/0/4", "et-0/0/5"));
    {
      String ifaceName = "et-0/0/2";
      String unitName = String.format("%s.0", ifaceName);
      org.batfish.representation.juniper.Interface iface =
          vc.getMasterLogicalSystem().getInterfaces().get(ifaceName);

      assertThat(iface.getUnits(), hasKeys(unitName));
      org.batfish.representation.juniper.Interface unit = iface.getUnits().get(unitName);

      assertThat(unit.getEthernetSwitching().getSwitchportMode(), equalTo(SwitchportMode.ACCESS));
      VlanReference member =
          (VlanReference) getOnlyElement(unit.getEthernetSwitching().getVlanMembers());
      assertThat(member.getName(), equalTo("TWO"));
    }
    {
      String ifaceName = "et-0/0/3";
      String unitName = String.format("%s.0", ifaceName);
      org.batfish.representation.juniper.Interface iface =
          vc.getMasterLogicalSystem().getInterfaces().get(ifaceName);

      assertThat(iface.getUnits(), hasKeys(unitName));
      org.batfish.representation.juniper.Interface unit = iface.getUnits().get(unitName);

      assertThat(unit.getEthernetSwitching().getSwitchportMode(), equalTo(SwitchportMode.ACCESS));
      VlanRange member = (VlanRange) getOnlyElement(unit.getEthernetSwitching().getVlanMembers());
      assertThat(member.getRange(), equalTo(IntegerSpace.of(2)));
    }
    {
      String ifaceName = "et-0/0/4";
      String unitName = String.format("%s.0", ifaceName);
      org.batfish.representation.juniper.Interface iface =
          vc.getMasterLogicalSystem().getInterfaces().get(ifaceName);

      assertThat(iface.getUnits(), hasKeys(unitName));
      org.batfish.representation.juniper.Interface unit = iface.getUnits().get(unitName);

      assertThat(unit.getEthernetSwitching().getSwitchportMode(), equalTo(SwitchportMode.ACCESS));
    }
    {
      String ifaceName = "et-0/0/5";
      String unitName = String.format("%s.0", ifaceName);
      org.batfish.representation.juniper.Interface iface =
          vc.getMasterLogicalSystem().getInterfaces().get(ifaceName);

      assertThat(iface.getUnits(), hasKeys(unitName));
      org.batfish.representation.juniper.Interface unit = iface.getUnits().get(unitName);

      assertThat(unit.getEthernetSwitching().getSwitchportMode(), equalTo(SwitchportMode.ACCESS));
      VlanReference member =
          (VlanReference) getOnlyElement(unit.getEthernetSwitching().getVlanMembers());
      assertThat(member.getName(), equalTo("NOTSET"));
    }
  }

  @Test
  public void testVlanAccessConversion() {
    Configuration c = parseConfig("juniper-vlan-access");

    assertThat(c, hasInterface("et-0/0/2.0", isSwitchport()));
    assertThat(c, hasInterface("et-0/0/2.0", hasSwitchPortMode(SwitchportMode.ACCESS)));
    assertThat(c, hasInterface("et-0/0/2.0", hasAccessVlan(2)));

    assertThat(c, hasInterface("et-0/0/3.0", isSwitchport()));
    assertThat(c, hasInterface("et-0/0/3.0", hasSwitchPortMode(SwitchportMode.ACCESS)));
    assertThat(c, hasInterface("et-0/0/3.0", hasAccessVlan(2)));

    assertThat(c, hasInterface("et-0/0/4.0", isSwitchport()));
    assertThat(c, hasInterface("et-0/0/4.0", hasSwitchPortMode(SwitchportMode.ACCESS)));
    assertThat(c, hasInterface("et-0/0/4.0", hasAccessVlan(1)));

    assertThat(c, hasInterface("et-0/0/5.0", isSwitchport(false)));
    assertThat(c, hasInterface("et-0/0/5.0", hasSwitchPortMode(SwitchportMode.NONE)));
    assertThat(c, hasInterface("et-0/0/5.0", hasAccessVlan(nullValue())));
  }

  @Test
  public void testVlanAllExtraction() {
    JuniperConfiguration vc = parseJuniperConfig("juniper-vlan-all");

    String ifaceName = "et-0/0/0";
    assertThat(vc.getMasterLogicalSystem().getInterfaces(), hasKeys(ifaceName));
    String unitName = String.format("%s.0", ifaceName);
    org.batfish.representation.juniper.Interface iface =
        vc.getMasterLogicalSystem().getInterfaces().get(ifaceName);

    assertThat(iface.getUnits(), hasKeys(unitName));
    org.batfish.representation.juniper.Interface unit = iface.getUnits().get(unitName);

    assertThat(unit.getEthernetSwitching().getSwitchportMode(), equalTo(SwitchportMode.TRUNK));
    assertThat(
        getOnlyElement(unit.getEthernetSwitching().getVlanMembers()), instanceOf(AllVlans.class));
  }

  @Test
  public void testVlanAllConversion() {
    Configuration c = parseConfig("juniper-vlan-all");

    assertThat(c, hasInterface("et-0/0/0.0", isSwitchport()));
    assertThat(c, hasInterface("et-0/0/0.0", hasSwitchPortMode(SwitchportMode.TRUNK)));
    assertThat(c, hasInterface("et-0/0/0.0", hasNativeVlan(nullValue())));
    assertThat(
        c,
        hasInterface(
            "et-0/0/0.0", hasAllowedVlans(equalTo(IntegerSpace.of(Range.closed(1, 4094))))));
  }

  @Test
  public void testVxlanL2vniConversion() {
    Configuration c = parseConfig("juniper-vxlan-l2vni");

    assertThat(c, hasInterface("xe-0/0/0.0", isSwitchport()));
    assertThat(c, hasInterface("xe-0/0/0.0", hasSwitchPortMode(SwitchportMode.ACCESS)));
    assertEquals(c.getDefaultVrf().getLayer2Vnis().get(5010).getVlan(), 10);
    assertNull(c.getDefaultVrf().getLayer2Vnis().get(5010).getSourceAddress());
    assertEquals(c.getDefaultVrf().getLayer2Vnis().get(5010).getSrcVrf(), "default");
    assertEquals(c.getDefaultVrf().getLayer2Vnis().get(5010).getUdpPort(), 4789);
    assertEquals(c.getDefaultVrf().getLayer2Vnis().get(5020).getVlan(), 20);
    assertNull(c.getDefaultVrf().getLayer2Vnis().get(5020).getSourceAddress());
    assertEquals(c.getDefaultVrf().getLayer2Vnis().get(5020).getSrcVrf(), "default");
    assertEquals(c.getDefaultVrf().getLayer2Vnis().get(5020).getUdpPort(), 4789);
  }

  @Test
  public void testVxlanL3vniConversion() {
    Configuration c = parseConfig("juniper-vxlan-l3vni");

    assertThat(c, hasInterface("xe-0/0/0.0", isSwitchport()));
    assertThat(c, hasInterface("xe-0/0/0.0", hasSwitchPortMode(SwitchportMode.ACCESS)));
    assertEquals(
        c.getDefaultVrf().getLayer3Vnis().get(5010).getSourceAddress(), Ip.parse("10.0.1.111"));
    assertEquals(c.getDefaultVrf().getLayer3Vnis().get(5010).getSrcVrf(), "default");
    assertEquals(c.getDefaultVrf().getLayer3Vnis().get(5010).getUdpPort(), 4789);
    assertEquals(
        c.getDefaultVrf().getLayer3Vnis().get(5020).getSourceAddress(), Ip.parse("10.0.2.111"));
    assertEquals(c.getDefaultVrf().getLayer3Vnis().get(5020).getSrcVrf(), "default");
    assertEquals(c.getDefaultVrf().getLayer3Vnis().get(5020).getUdpPort(), 4789);
  }

  /** Test that interfaces inherit OSPF settings inside a routing instance. */
  @Test
  public void testIsisInterfaceAll() {
    String hostname = "isis-interface-all";
    Configuration c = parseConfig(hostname);

    // ge-0/0/0.0 does not inherit from "all" -- both level1 and level2 enabled
    assertThat(c.getAllInterfaces().get("ge-0/0/0.0").getIsis().getLevel1(), notNullValue());
    assertThat(c.getAllInterfaces().get("ge-0/0/0.0").getIsis().getLevel2(), notNullValue());

    // ge-0/0/1.0 does not inherit from "all" -- level 1 disabled, level 2 enabled
    assertThat(c.getAllInterfaces().get("ge-0/0/1.0").getIsis().getLevel1(), nullValue());
    assertThat(c.getAllInterfaces().get("ge-0/0/1.0").getIsis().getLevel2(), notNullValue());

    // ge-0/0/2.0 inherits from "all" -- level 1 is enabled, level 2 is disabled
    assertThat(c.getAllInterfaces().get("ge-0/0/2.0").getIsis().getLevel1(), notNullValue());
    assertThat(c.getAllInterfaces().get("ge-0/0/2.0").getIsis().getLevel2(), nullValue());

    // ge-0/0/3.0 does not inherit from "all" (different routing instance) -- level 1 is disabled,
    // level 2 enabled
    assertThat(c.getAllInterfaces().get("ge-0/0/3.0").getIsis().getLevel1(), nullValue());
    assertThat(c.getAllInterfaces().get("ge-0/0/3.0").getIsis().getLevel2(), notNullValue());
  }

  /** Test that interfaces inherit OSPF settings inside a routing instance. */
  @Test
  public void testOspfInterfaceAll() {
    String hostname = "ospf-area-interface-all";
    Configuration c = parseConfig(hostname);

    // ge-0/0/0.0 does not inherit from "all" (cost 1 is default for xe)
    assertThat(c, hasInterface("ge-0/0/0.0", hasOspfCost(equalTo(1))));

    // ge-0/0/1.0 does not inherit from "all"
    // has its own hello-interval
    assertThat(c, hasInterface("ge-0/0/1.0", hasOspfCost(equalTo(1))));
    assertThat(
        c.getAllInterfaces().get("ge-0/0/1.0").getOspfSettings().getHelloInterval(), equalTo(20));

    // ge-0/0/2.0 inherits from "all"
    assertThat(c, hasInterface("ge-0/0/2.0", hasOspfCost(equalTo(111))));
    assertThat(
        c.getAllInterfaces().get("ge-0/0/2.0").getOspfSettings().getNetworkType(),
        equalTo(OspfNetworkType.POINT_TO_POINT));
    assertThat(
        c.getAllInterfaces().get("ge-0/0/2.0").getOspfSettings().getHelloInterval(), equalTo(222));
    assertThat(
        c.getAllInterfaces().get("ge-0/0/2.0").getOspfSettings().getDeadInterval(), equalTo(333));

    // ge-0/0/3.0 does not inherit from "all" (different routing instance)
    assertThat(c, hasInterface("ge-0/0/3.0", hasOspfCost(equalTo(1))));
  }

  /** Test of bfe#8498 crash. */
  @Test
  public void testOspfInterfaceAllCrash() {
    parseConfig("ospf-area-interface-all-crash");
  }

  /** Test that using the physical interface in OSPF context maps things to unit 0 */
  @Test
  public void testOspfImplicitUnit0() {
    String hostname = "ospf-implicit-unit0";
    Configuration c = parseConfig(hostname);
    assertThat(c, hasInterface("ge-0/0/0.0", hasOspfCost(equalTo(110))));
  }

  @Test
  public void testOspfAreaInterfaceAllDuplicateError() throws IOException {
    String hostname = "ospf-area-interface-all-duplicate-error";
    String fileKey = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    batfish.loadConfigurations(batfish.getSnapshot());
    ParseVendorConfigurationAnswerElement pvcae =
        batfish.loadParseVendorConfigurationAnswerElement(batfish.getSnapshot());

    assertThat(
        pvcae.getWarnings().get(fileKey).getFatalRedFlagWarnings(),
        hasItem(
            WarningMatchers.hasText(
                containsString("Interface \"all\" assigned to multiple areas"))));
  }

  @Test
  public void testOspfAreaInterfaceDuplicateError() throws IOException {
    String hostname = "ospf-area-interface-duplicate-error";
    String fileKey = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    batfish.loadConfigurations(batfish.getSnapshot());
    ParseVendorConfigurationAnswerElement pvcae =
        batfish.loadParseVendorConfigurationAnswerElement(batfish.getSnapshot());

    assertThat(
        pvcae.getWarnings().get(fileKey).getFatalRedFlagWarnings(),
        hasItem(
            WarningMatchers.hasText(
                containsString("Interface \"ge-0/0/0.0\" assigned to multiple areas"))));
  }

  @Test
  public void testOspfAreaInterfaceIpMissing() throws IOException {
    String hostname = "ospf-area-interface-ip-missing";
    String fileKey = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    batfish.loadConfigurations(batfish.getSnapshot());
    ParseVendorConfigurationAnswerElement pvcae =
        batfish.loadParseVendorConfigurationAnswerElement(batfish.getSnapshot());

    assertThat(pvcae.getWarnings().get(fileKey).getFatalRedFlagWarnings(), empty());
  }

  @Test
  public void testConditionExtraction() {
    String hostname = "juniper-condition";
    JuniperConfiguration jc = parseJuniperConfig(hostname);
    assertThat(
        jc.getMasterLogicalSystem()
            .getPolicyStatements()
            .get("p1")
            .getTerms()
            .get("t1")
            .getFroms()
            .getFromConditions(),
        contains(new PsFromCondition("c1")));
    assertThat(
        jc.getMasterLogicalSystem().getConditions(),
        hasKeys("c1", "c2", "c3", "c4", "c5", "c6", "c7"));
    {
      Condition c = jc.getMasterLogicalSystem().getConditions().get("c1");
      assertThat(c.getIfRouteExists(), notNullValue());
      assertThat(c.getIfRouteExists().getPrefix(), equalTo(Prefix.strict("1.0.0.0/24")));
      assertThat(c.getIfRouteExists().getTable(), equalTo("inet.0"));
    }
    {
      Condition c = jc.getMasterLogicalSystem().getConditions().get("c2");
      assertThat(c.getIfRouteExists(), notNullValue());
      assertThat(c.getIfRouteExists().getPrefix(), equalTo(Prefix.strict("2.0.0.0/24")));
      assertThat(c.getIfRouteExists().getTable(), equalTo("ri2.inet.0"));
    }
    {
      Condition c = jc.getMasterLogicalSystem().getConditions().get("c3");
      assertThat(c.getIfRouteExists(), notNullValue());
      assertThat(c.getIfRouteExists().getPrefix(), equalTo(Prefix.strict("3.0.0.0/24")));
      assertThat(c.getIfRouteExists().getTable(), equalTo("ri3.inet.0"));
    }
    {
      Condition c = jc.getMasterLogicalSystem().getConditions().get("c4");
      assertThat(c.getIfRouteExists(), notNullValue());
      assertThat(c.getIfRouteExists().getPrefix(), nullValue());
      assertThat(c.getIfRouteExists().getPrefix6(), nullValue());
      assertThat(c.getIfRouteExists().getTable(), nullValue());
    }
    {
      Condition c = jc.getMasterLogicalSystem().getConditions().get("c5");
      assertThat(c.getIfRouteExists(), notNullValue());
      assertThat(c.getIfRouteExists().getPrefix6(), equalTo(Prefix6.parse("::1.2.3.4/127")));
    }
    {
      Condition c = jc.getMasterLogicalSystem().getConditions().get("c6");
      assertThat(c.getIfRouteExists(), notNullValue());
      assertThat(c.getIfRouteExists().getPrefix(), equalTo(Prefix.parse("192.0.2.1/32")));
    }
    {
      Condition c = jc.getMasterLogicalSystem().getConditions().get("c7");
      assertThat(c.getIfRouteExists(), notNullValue());
      assertThat(
          c.getIfRouteExists().getPrefix6(),
          equalTo(Prefix6.parse("2001:db8:1234:5678:abc1:2345:6789:abcd/128")));
    }
  }

  @Test
  public void testConditionConversion() {
    String hostname = "juniper-condition";
    String c1TrackName = computeConditionTrackName("c1");
    String c2TrackName = computeConditionTrackName("c2");
    String c3TrackName = computeConditionTrackName("c3");
    String c4TrackName = computeConditionTrackName("c4");
    String c5TrackName = computeConditionTrackName("c5");
    String c6TrackName = computeConditionTrackName("c6");
    String c7TrackName = computeConditionTrackName("c7");
    Configuration c = parseConfig(hostname);

    // Conditions should be converted to tracks
    assertThat(
        c.getTrackingGroups(),
        hasKeys(
            c1TrackName,
            c2TrackName,
            c3TrackName,
            c4TrackName,
            c5TrackName,
            c6TrackName,
            c7TrackName));
    assertThat(
        c.getTrackingGroups().get(c1TrackName),
        equalTo(
            TrackMethods.route(Prefix.strict("1.0.0.0/24"), ImmutableSet.of(), DEFAULT_VRF_NAME)));
    assertThat(
        c.getTrackingGroups().get(c2TrackName),
        equalTo(TrackMethods.route(Prefix.strict("2.0.0.0/24"), ImmutableSet.of(), "ri2")));
    assertThat(
        c.getTrackingGroups().get(c3TrackName),
        equalTo(TrackMethods.route(Prefix.strict("3.0.0.0/24"), ImmutableSet.of(), "ri3")));
    assertThat(c.getTrackingGroups().get(c4TrackName), equalTo(TrackMethods.alwaysTrue()));
    assertThat(c.getTrackingGroups().get(c5TrackName), equalTo(TrackMethods.alwaysFalse()));
    assertThat(
        c.getTrackingGroups().get(c6TrackName),
        equalTo(TrackMethods.route(Prefix.strict("192.0.2.1/32"), ImmutableSet.of(), "default")));
    assertThat(c.getTrackingGroups().get(c7TrackName), equalTo(TrackMethods.alwaysFalse()));

    // BGP process should watch tracks for conditions
    assertThat(
        c.getDefaultVrf().getBgpProcess().getTracks(),
        containsInAnyOrder(
            c1TrackName,
            c2TrackName,
            c3TrackName,
            c4TrackName,
            c5TrackName,
            c6TrackName,
            c7TrackName));
  }

  @Test
  public void testResolutionExtraction() {
    String hostname = "juniper-resolution";
    JuniperConfiguration jc = parseJuniperConfig(hostname);
    Resolution r = jc.getMasterLogicalSystem().getDefaultRoutingInstance().getResolution();

    assertNotNull(r);

    ResolutionRib rr = r.getRib();

    assertNotNull(rr);
    assertThat(rr.getName(), equalTo(RoutingInformationBase.RIB_IPV4_UNICAST));
    assertThat(rr.getImportPolicies(), contains("respol"));
    assertThat(jc.getMasterLogicalSystem().getPolicyStatements(), hasKeys(("respol")));
  }

  @Test
  public void testResolutionConversion() {
    String hostname = "juniper-resolution";
    Configuration c = parseConfig(hostname);
    String resolutionPolicyName = generateResolutionRibImportPolicyName(DEFAULT_VRF_NAME);

    assertThat(c.getDefaultVrf().getResolutionPolicy(), equalTo(resolutionPolicyName));
    assertThat(c.getRoutingPolicies(), hasKey(resolutionPolicyName));

    RoutingPolicy r = c.getRoutingPolicies().get(resolutionPolicyName);

    assertTrue(r.processReadOnly(new ConnectedRoute(Prefix.create(ZERO, 24), "blah")));
    assertFalse(r.processReadOnly(new ConnectedRoute(Prefix.create(ZERO, 25), "blah")));
  }

  @Test
  public void testVrrpExtraction() {
    String hostname = "juniper-vrrp";
    JuniperConfiguration vc = parseJuniperConfig(hostname);
    org.batfish.representation.juniper.Interface i =
        vc.getMasterLogicalSystem().getInterfaces().get("xe-0/0/0").getUnits().get("xe-0/0/0.0");
    assertThat(i.getVrrpGroups(), hasKeys(1, 2));
    {
      VrrpGroup v = i.getVrrpGroups().get(1);
      assertThat(v.getSourceAddress(), equalTo(ConcreteInterfaceAddress.parse("10.0.0.1/24")));
      assertThat(v.getVirtualAddresses(), contains(Ip.parse("10.0.0.2")));
      assertTrue(v.getPreempt());
    }
    {
      VrrpGroup v = i.getVrrpGroups().get(2);
      assertThat(v.getSourceAddress(), equalTo(ConcreteInterfaceAddress.parse("10.0.1.1/24")));
      assertThat(
          v.getVirtualAddresses(), containsInAnyOrder(Ip.parse("10.0.1.2"), Ip.parse("10.0.1.3")));
      assertFalse(v.getPreempt());
    }
  }

  @Test
  public void testVrrpConversion() {
    String hostname = "juniper-vrrp";
    Configuration c = parseConfig(hostname);
    Interface i = c.getAllInterfaces().get("xe-0/0/0.0");
    assertThat(i.getVrrpGroups(), hasKeys(1, 2));
    {
      org.batfish.datamodel.VrrpGroup v = i.getVrrpGroups().get(1);
      assertThat(v.getSourceAddress(), equalTo(ConcreteInterfaceAddress.parse("10.0.0.1/24")));
      assertThat(v.getVirtualAddresses(), hasKeys(i.getName()));
      assertThat(v.getVirtualAddresses().get(i.getName()), contains(Ip.parse("10.0.0.2")));
      assertTrue(v.getPreempt());
    }
    {
      org.batfish.datamodel.VrrpGroup v = i.getVrrpGroups().get(2);
      assertThat(v.getSourceAddress(), equalTo(ConcreteInterfaceAddress.parse("10.0.1.1/24")));
      assertThat(v.getVirtualAddresses(), hasKeys(i.getName()));
      assertThat(
          v.getVirtualAddresses().get(i.getName()),
          containsInAnyOrder(Ip.parse("10.0.1.2"), Ip.parse("10.0.1.3")));
      assertFalse(v.getPreempt());
    }
  }

  @Test
  public void testVrrpErrorMultipleSourceAddressesForVrid() throws IOException {
    String hostname = "juniper-vrrp-error-multiple-source-addresses-for-vrid";
    String fileKey = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ParseVendorConfigurationAnswerElement pvcae =
        batfish.loadParseVendorConfigurationAnswerElement(batfish.getSnapshot());
    assertThat(
        pvcae.getWarnings().get(fileKey),
        WarningsMatchers.hasRedFlag(
            WarningMatchers.hasText(
                "FATAL: Multiple inet addresses with the same VRRP VRID 1 on interface"
                    + " 'xe-0/0/0.0'")));
  }

  @Test
  public void testVrrpErrorVirtualAddressOutsideSourceAddressSubnet() throws IOException {
    String hostname = "juniper-vrrp-error-virtual-address-outside-source-address-subnet";
    String fileKey = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    batfish.loadConfigurations(batfish.getSnapshot());
    ParseVendorConfigurationAnswerElement pvcae =
        batfish.loadParseVendorConfigurationAnswerElement(batfish.getSnapshot());
    assertThat(
        pvcae.getWarnings().get(fileKey),
        WarningsMatchers.hasRedFlag(
            WarningMatchers.hasText(
                "FATAL: Cannot assign virtual-address 10.0.1.2 outside of subnet for inet address"
                    + " 10.0.0.1/24")));
  }

  @Test
  public void testVrrpErrorNoVirtualAddress() throws IOException {
    String hostname = "juniper-vrrp-error-no-virtual-address";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    assertThat(
        ccae,
        hasRedFlagWarning(
            hostname,
            equalTo(
                "FATAL: Cannot create VRRP group for vrid 1 on interface 'xe-0/0/0.0' because no"
                    + " virtual-address is assigned.")));
  }

  @Test
  public void testIgnoredProtocols() {
    // don't crash
    parseJuniperConfig("ignored-protocols");
  }

  @Test
  public void testMaximumPrefixes() throws IOException {
    String hostname = "maximum-prefixes";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ParseVendorConfigurationAnswerElement pvcae =
        batfish.loadParseVendorConfigurationAnswerElement(batfish.getSnapshot());
    assertThat(
        pvcae,
        hasParseWarning("configs/" + hostname, equalTo("Batfish does not limit maximum-prefixes")));
  }

  @Test
  public void testIgnoredSystem() {
    // don't crash
    parseJuniperConfig("ignored-system");
  }

  @Test
  public void testIgnoredClassOfService() {
    // don't crash
    parseJuniperConfig("ignored-class-of-service");
  }

  @Test
  public void testClassOfServiceCodePointAliases() throws IOException {
    String hostname = "class-of-service-code-point-aliases";
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ParseVendorConfigurationAnswerElement pvcae =
        batfish.loadParseVendorConfigurationAnswerElement(batfish.getSnapshot());

    assertThat(
        pvcae.getWarnings().get(filename).getParseWarnings(),
        containsInAnyOrder(
            hasComment(
                "200000 is not a legal code-point. Must be of form xxxxxx, where x is 1 or 0."),
            hasComment(
                "1010101 is not a legal code-point. Must be of form xxxxxx, where x is 1 or 0.")));

    assertThat(
        ((JuniperConfiguration)
                batfish.loadVendorConfigurations(batfish.getSnapshot()).get(hostname))
            .getMasterLogicalSystem()
            .getDscpAliases(),
        equalTo(ImmutableMap.of("my1", 3)));

    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    assertThat(
        ccae,
        hasDefinedStructureWithDefinitionLines(
            filename, CLASS_OF_SERVICE_CODE_POINT_ALIAS, "my1", contains(4)));

    assertThat(ccae, hasNumReferrers(filename, CLASS_OF_SERVICE_CODE_POINT_ALIAS, "my1", 1));
  }

  @Test
  public void testFirewallFilterFromExtraction() {
    String hostname = "firewall-filter-from-port";
    JuniperConfiguration vc = parseJuniperConfig(hostname);
    Map<String, FirewallFilter> filters = vc.getMasterLogicalSystem().getFirewallFilters();

    assertThat(filters, hasEntry(equalTo("f1"), instanceOf(ConcreteFirewallFilter.class)));

    ConcreteFirewallFilter filter = (ConcreteFirewallFilter) filters.get("f1");

    assertThat(filter.getTerms(), hasKeys("t1"));

    FwTerm term = filter.getTerms().get("t1");

    Iterator<FwFrom> i = term.getFroms().iterator();
    assertTrue(i.hasNext());
    {
      FwFrom from = i.next();
      assertThat(from, instanceOf(FwFromPort.class));
      FwFromPort fromPort = (FwFromPort) from;
      assertThat(fromPort.getPortRange(), equalTo(SubRange.singleton(NamedPort.BGP.number())));
    }
    {
      FwFrom from = i.next();
      assertThat(from, instanceOf(FwFromPort.class));
      FwFromPort fromPort = (FwFromPort) from;
      // leading zeros should be ignored
      assertThat(fromPort.getPortRange(), equalTo(SubRange.singleton(100)));
    }
    {
      FwFrom from = i.next();
      assertThat(from, instanceOf(FwFromPort.class));
      FwFromPort fromPort = (FwFromPort) from;
      assertThat(fromPort.getPortRange(), equalTo(new SubRange(1000, 2000)));
    }
    {
      FwFrom from = i.next();
      assertThat(from, instanceOf(FwFromDestinationPort.class));
      FwFromDestinationPort fromPort = (FwFromDestinationPort) from;
      assertThat(fromPort.getPortRange(), equalTo(SubRange.singleton(NamedPort.BGP.number())));
    }
    {
      FwFrom from = i.next();
      assertThat(from, instanceOf(FwFromDestinationPort.class));
      FwFromDestinationPort fromPort = (FwFromDestinationPort) from;
      assertThat(fromPort.getPortRange(), equalTo(SubRange.singleton(100)));
    }
    {
      FwFrom from = i.next();
      assertThat(from, instanceOf(FwFromDestinationPort.class));
      FwFromDestinationPort fromPort = (FwFromDestinationPort) from;
      assertThat(fromPort.getPortRange(), equalTo(new SubRange(1000, 2000)));
    }
    {
      FwFrom from = i.next();
      assertThat(from, instanceOf(FwFromSourcePort.class));
      FwFromSourcePort fromPort = (FwFromSourcePort) from;
      assertThat(fromPort.getPortRange(), equalTo(SubRange.singleton(NamedPort.BGP.number())));
    }
    {
      FwFrom from = i.next();
      assertThat(from, instanceOf(FwFromSourcePort.class));
      FwFromSourcePort fromPort = (FwFromSourcePort) from;
      assertThat(fromPort.getPortRange(), equalTo(SubRange.singleton(100)));
    }
    {
      FwFrom from = i.next();
      assertThat(from, instanceOf(FwFromSourcePort.class));
      FwFromSourcePort fromPort = (FwFromSourcePort) from;
      assertThat(fromPort.getPortRange(), equalTo(new SubRange(1000, 2000)));
    }
    assertFalse(i.hasNext());
  }

  @Test
  public void testApplyGroupsParsing() {
    String hostname = "apply-groups";
    parseJuniperConfig(hostname);
  }

  @Test
  public void testApplyGroupsRoutingInstancesParsing() {
    Configuration c = parseConfig("apply-groups-routing-instances");
    assertThat(c, hasVrf("FOO", hasStaticRoutes(contains(hasPrefix(Prefix.parse("1.1.1.0/24"))))));
  }

  @Test
  public void testDefineStructureFromNested() throws IOException {
    String hostname = "define-structure-from-nested";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());
    assertThat(
        ccae,
        hasDefinedStructureWithDefinitionLines(
            "configs/" + hostname,
            FIREWALL_FILTER_TERM,
            "foo default-deny-udp",
            equalTo(IntegerSpace.unionOf(new SubRange(8, 19)).enumerate())));
  }

  @Test
  public void testAsPathExpandExtraction() {
    String hostname = "juniper_as_path_expand";
    JuniperConfiguration vc = parseJuniperConfig(hostname);

    assertThat(
        vc.getMasterLogicalSystem().getPolicyStatements(),
        hasKeys("last-as-no-count", "last-as-count-2", "as-list", "expand-then-prepend"));
    {
      PolicyStatement ps =
          vc.getMasterLogicalSystem().getPolicyStatements().get("last-as-no-count");
      assertThat(ps.getTerms(), aMapWithSize(1));
      PsTerm term = Iterables.getOnlyElement(ps.getTerms().values());
      assertThat(
          term.getThens().getAllThens(), contains(instanceOf(PsThenAsPathExpandLastAs.class)));
      PsThenAsPathExpandLastAs then =
          (PsThenAsPathExpandLastAs) Iterables.getOnlyElement(term.getThens().getAllThens());
      assertThat(then.getCount(), equalTo(1));
    }
    {
      PolicyStatement ps = vc.getMasterLogicalSystem().getPolicyStatements().get("last-as-count-2");
      assertThat(ps.getTerms(), aMapWithSize(1));
      PsTerm term = Iterables.getOnlyElement(ps.getTerms().values());
      assertThat(
          term.getThens().getAllThens(), contains(instanceOf(PsThenAsPathExpandLastAs.class)));
      PsThenAsPathExpandLastAs then =
          (PsThenAsPathExpandLastAs) Iterables.getOnlyElement(term.getThens().getAllThens());
      assertThat(then.getCount(), equalTo(2));
    }
    {
      PolicyStatement ps = vc.getMasterLogicalSystem().getPolicyStatements().get("as-list");
      assertThat(ps.getTerms(), aMapWithSize(1));
      PsTerm term = Iterables.getOnlyElement(ps.getTerms().values());
      assertThat(
          term.getThens().getAllThens(), contains(instanceOf(PsThenAsPathExpandAsList.class)));
      PsThenAsPathExpandAsList then =
          (PsThenAsPathExpandAsList) Iterables.getOnlyElement(term.getThens().getAllThens());
      assertThat(then.getAsList(), contains(123L, (456L << 16) + 789L));
    }
    {
      PolicyStatement ps =
          vc.getMasterLogicalSystem().getPolicyStatements().get("expand-then-prepend");
      assertThat(ps.getTerms(), aMapWithSize(1));
      PsTerm term = Iterables.getOnlyElement(ps.getTerms().values());
      assertThat(
          term.getThens().getAllThens(),
          contains(
              // reverse of declared order is expected
              instanceOf(PsThenAsPathPrepend.class), instanceOf(PsThenAsPathExpandAsList.class)));

      PsThenAsPathPrepend prepend =
          (PsThenAsPathPrepend) Iterables.get(term.getThens().getAllThens(), 0);
      assertThat(prepend.getAsList(), contains(456L));

      PsThenAsPathExpandAsList expand =
          (PsThenAsPathExpandAsList) Iterables.get(term.getThens().getAllThens(), 1);
      assertThat(expand.getAsList(), contains(123L));
    }
  }

  @Test
  public void testAsPathExpandConversion() {
    String hostname = "juniper_as_path_expand";
    Configuration c = parseConfig(hostname);

    Bgpv4Route inputRouteNonEmptyAsPath =
        Bgpv4Route.builder()
            .setAsPath(AsPath.ofSingletonAsSets(1L, 2L))
            .setOriginatorIp(Ip.ZERO)
            .setOriginMechanism(LEARNED)
            .setOriginType(OriginType.IGP)
            .setReceivedFrom(ReceivedFromIp.of(Ip.parse("192.0.2.1")))
            .setNetwork(Prefix.ZERO)
            .setProtocol(RoutingProtocol.BGP)
            .setNextHop(NextHopDiscard.instance())
            .build();
    Bgpv4Route inputRouteEmptyAsPath =
        inputRouteNonEmptyAsPath.toBuilder().setAsPath(AsPath.empty()).build();
    assertThat(
        c.getRoutingPolicies(),
        hasKeys("last-as-no-count", "last-as-count-2", "as-list", "expand-then-prepend"));
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("last-as-no-count");
      Bgpv4Route inputRoute = inputRouteNonEmptyAsPath;
      Bgpv4Route.Builder outputRoute = inputRoute.toBuilder();
      rp.process(inputRoute, outputRoute, IN);
      assertThat(outputRoute.getAsPath(), equalTo(AsPath.ofSingletonAsSets(1L, 1L, 2L)));
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("last-as-no-count");
      Bgpv4Route inputRoute = inputRouteEmptyAsPath;
      Bgpv4Route.Builder outputRoute = inputRoute.toBuilder();
      rp.process(inputRoute, outputRoute, IN);
      assertThat(outputRoute.getAsPath(), equalTo(AsPath.empty()));
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("last-as-count-2");
      Bgpv4Route inputRoute = inputRouteNonEmptyAsPath;
      Bgpv4Route.Builder outputRoute = inputRoute.toBuilder();
      rp.process(inputRoute, outputRoute, IN);
      assertThat(outputRoute.getAsPath(), equalTo(AsPath.ofSingletonAsSets(1L, 1L, 1L, 2L)));
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("as-list");
      Bgpv4Route inputRoute = inputRouteNonEmptyAsPath;
      Bgpv4Route.Builder outputRoute = inputRoute.toBuilder();
      rp.process(inputRoute, outputRoute, IN);
      assertThat(
          outputRoute.getAsPath(),
          equalTo(AsPath.ofSingletonAsSets(123L, (456L << 16) + 789L, 1L, 2L)));
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("expand-then-prepend");
      Bgpv4Route inputRoute = inputRouteEmptyAsPath;
      Bgpv4Route.Builder outputRoute = inputRoute.toBuilder();
      rp.process(inputRoute, outputRoute, IN);
      // prepend 456
      // expand 123
      // The prepend is applied before the expand, even though it is declared after.
      assertThat(outputRoute.getAsPath(), equalTo(AsPath.ofSingletonAsSets(123L, 456L)));
    }
  }

  @Test
  public void testNestedMultilineComments() {
    String hostname = "juniper_nested_multiline_comments";
    // don't crash
    parseConfig(hostname);
  }

  @Test
  public void testInactiveInterfaceRoutes() throws IOException {
    Configuration c = parseConfig("inactive_interface_local_route");
    Batfish batfish =
        BatfishTestUtils.getBatfish(ImmutableSortedMap.of(c.getHostname(), c), _folder);
    batfish.computeDataPlane(batfish.getSnapshot());
    DataPlane dp = batfish.loadDataPlane(batfish.getSnapshot());
    assertThat(
        dp.getRibs().get(c.getHostname(), c.getDefaultVrf().getName()).getRoutes(),
        contains(
            allOf(hasPrefix(Prefix.parse("1.1.1.1/32")), hasNextHop(NextHopDiscard.instance()))));
  }

  @Test
  public void testInterfaceMediaTypes() {
    // don't crash
    parseConfig("interface-media-types");
  }

  @Test
  public void testBridgeDomainsExtraction() {
    String hostname = "bridge-domains";
    JuniperConfiguration vc = parseJuniperConfig(hostname);
    assertThat(
        vc.getMasterLogicalSystem().getDefaultRoutingInstance().getBridgeDomains(),
        hasKeys("bd1", "bd3", "bd4", "bd5"));
    assertThat(
        vc.getMasterLogicalSystem().getRoutingInstances().get("ri1").getBridgeDomains(),
        hasKeys("bd2"));
    {
      BridgeDomain bd =
          vc.getMasterLogicalSystem().getDefaultRoutingInstance().getBridgeDomains().get("bd1");
      assertThat(bd.getRoutingInterface(), equalTo("irb.1"));
      assertThat(bd.getVlanId(), equalTo(BridgeDomainVlanIdNumber.of(1)));
    }
    // TODO: extractions for interface, vlan-id-list, vlan-tags
    {
      BridgeDomain bd =
          vc.getMasterLogicalSystem()
              .getRoutingInstances()
              .get("ri1")
              .getBridgeDomains()
              .get("bd2");
      assertThat(bd.getRoutingInterface(), nullValue());
      assertThat(bd.getVlanId(), nullValue());
    }
    {
      BridgeDomain bd =
          vc.getMasterLogicalSystem().getDefaultRoutingInstance().getBridgeDomains().get("bd3");
      assertThat(bd.getRoutingInterface(), nullValue());
      assertThat(bd.getVlanId(), nullValue());
    }
    {
      BridgeDomain bd =
          vc.getMasterLogicalSystem().getDefaultRoutingInstance().getBridgeDomains().get("bd4");
      assertThat(bd.getRoutingInterface(), nullValue());
      assertThat(bd.getVlanId(), equalTo(BridgeDomainVlanIdNone.instance()));
    }
    {
      BridgeDomain bd =
          vc.getMasterLogicalSystem().getDefaultRoutingInstance().getBridgeDomains().get("bd5");
      assertThat(bd.getRoutingInterface(), nullValue());
      assertThat(bd.getVlanId(), equalTo(BridgeDomainVlanIdAll.instance()));
    }
  }

  @Test
  public void testBridgeDomainsConversion() {
    String hostname = "bridge-domains";
    Configuration c = parseConfig(hostname);
    assertThat(c, hasInterface("irb.1", allOf(hasVlan(1), isActive())));
    // TODO: Update assertions if we should instead require an active physical/l2 interface in the
    //       bridge domain. See JuniperConfiguration.convertBridgeDomain.
    assertFalse(c.getNormalVlanRange().contains(1));
  }

  @Test
  public void testBridgeDomainsReferences() throws IOException {
    String hostname = "bridge-domains";
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());
    assertThat(ccae, hasDefinedStructure(filename, BRIDGE_DOMAIN, "bd1"));
    // self refs
    assertThat(ccae, hasNumReferrers(filename, BRIDGE_DOMAIN, "bd1", 4));
    // self plus routing-interface
    assertThat(ccae, hasNumReferrers(filename, INTERFACE, "irb.1", 2));
  }

  private static final List<String> APPLY_PATH_NO_MATCH_CONFIGS =
      ImmutableList.of(
          "apply-path-no-match",
          "apply-path-no-match-not-ip-or-prefix",
          "apply-path-from-apply-groups-no-match");

  @Test
  public void testApplyPathNoMatchExtraction() {
    for (String hostname : APPLY_PATH_NO_MATCH_CONFIGS) {
      JuniperConfiguration vc = parseJuniperConfig(hostname);
      assertThat(vc.getMasterLogicalSystem().getPrefixLists().get("pl1").getPrefixes(), empty());
      assertFalse(vc.getMasterLogicalSystem().getPrefixLists().get("pl1").getHasIpv6());
    }
  }

  @Test
  public void testApplyPathNoMatchConversion() {
    for (String hostname : APPLY_PATH_NO_MATCH_CONFIGS) {
      Configuration c = parseConfig(hostname);
      // TODO: determine whether this should be empty or permit all
      assertThat(c.getRouteFilterLists().get("pl1").getLines(), empty());
    }
  }

  @Test
  public void testVlanForwardingOptionsDhcpSecurity() {
    // doesn't crash.
    parseJuniperConfig("vlan-forwarding-options");
  }

  @Test
  public void testApplyPathMixedIpAndNotIpOrPrefixExtraction() {
    String hostname = "apply-path-mixed-ip-and-not-ip-or-prefix";
    JuniperConfiguration vc = parseJuniperConfig(hostname);
    assertThat(
        vc.getMasterLogicalSystem().getPrefixLists().get("pl1").getPrefixes(),
        contains(Prefix.strict("192.0.2.1/32")));
    assertFalse(vc.getMasterLogicalSystem().getPrefixLists().get("pl1").getHasIpv6());
  }

  @Test
  public void testQuotingMistakesDontCrash() throws IOException {
    String hostname = "quote-mistakes";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ParseVendorConfigurationAnswerElement pvcae =
        batfish.loadParseVendorConfigurationAnswerElement(batfish.getSnapshot());
    assertThat(pvcae, hasParseWarning("configs/" + hostname, equalTo("Improperly-quoted string")));

    Configuration c = batfish.loadConfigurations(batfish.getSnapshot()).get(hostname);
    assertThat(
        c, hasInterface("xe-0/0/0", hasDescription("\"foo bar\" unit 0 ip address 1.2.3.4/31")));
  }

  /**
   * Tests of {@link JuniperListPaths}.
   *
   * <p>The code underlying this is somewhat complicated, and in an early version we found that it
   * crashed when the entire structure is defined in a group (because of generated code lines
   * created to ensure the relevant node in the syntax tree is present). This test is dedicated to
   * exercising this case for all paths in there.
   */
  @Test
  public void testApplyGroupsLists() {
    // Simply tests that all the lines parse without errors
    parseConfig("apply-groups-lists");
  }

  /**
   * TODO: Fix and un-xfail. To fix, should backtrack and try alternate (shallow, wildcard) paths
   * when inheriting groups lines and no match is found. See {@link GroupInheritor}.
   */
  @Test
  public void testApplyGroupsWildcardNestingExtraction() {
    String hostname = "wildcard-nesting";
    JuniperConfiguration vc = parseJuniperConfig(hostname);
    Set<ConcreteInterfaceAddress> assignedAddresses =
        vc.getMasterLogicalSystem()
            .getInterfaces()
            .get("xe-0/0/0")
            .getUnits()
            .get("xe-0/0/0.20")
            .getAllAddresses();
    assertThat(assignedAddresses, not(hasItem(ConcreteInterfaceAddress.parse("1.0.0.1/31"))));

    // TODO: fix and remove expected assertion error
    _thrown.expect(AssertionError.class);
    assertThat(assignedAddresses, hasItem(ConcreteInterfaceAddress.parse("2.0.0.1/31")));
  }

  @Test
  public void testPrefixExportLimit() throws IOException {
    String hostname = "prefix-export-limit";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ParseVendorConfigurationAnswerElement pvcae =
        batfish.loadParseVendorConfigurationAnswerElement(batfish.getSnapshot());
    assertThat(
        pvcae,
        hasParseWarning(
            "configs/" + hostname, equalTo("Batfish does not limit prefix-export-limit")));
  }

  @Test
  public void testFirewallInet6Filters() {
    // Should not crash.
    parseConfig("firewall-ipv6-filters");
  }

  @Test
  public void testSetPartial() {
    // Should not crash.
    parseConfig("set-partial");
  }

  @Test
  public void testInterfacesVlanMap() {
    // Should not crash.
    parseConfig("interfaces-vlan-map");
  }

  @Test
  public void testIsisImport() {
    // Should not crash.
    parseConfig("isis-import");
  }

  @Test
  public void testBmpStation() {
    // Should not crash.
    parseConfig("bmp-station");
  }

  @Test
  public void testMultipathResolve() {
    // Should not crash.
    parseConfig("multipath-resolve");
  }

  @Test
  public void testRecoveryTimeout() {
    // Should not crash.
    parseConfig("recovery-timeout");
  }

  @Test
  public void testJuniperAsPathExclamationRegex() {
    Configuration c = parseConfig("juniper-as-path-exclamation-regex");
    RoutingPolicy asPathGroupPolicy1 = c.getRoutingPolicies().get("AS_PATH_GROUP_POLICY1");
    Bgpv4Route.Builder test =
        Bgpv4Route.testBuilder()
            .setAdmin(100)
            .setNetwork(Prefix.parse("1.1.1.1/28"))
            .setOriginatorIp(Ip.parse("2.2.2.2"))
            .setOriginType(OriginType.INCOMPLETE)
            .setProtocol(RoutingProtocol.BGP);
    Result result =
        asPathGroupPolicy1.call(
            envWithRoute(c, test.setAsPath(AsPath.ofSingletonAsSets(1L)).build()));
    assertThat(result.getBooleanValue(), equalTo(false));

    result =
        asPathGroupPolicy1.call(
            envWithRoute(c, test.setAsPath(AsPath.ofSingletonAsSets(2L)).build()));
    assertThat(result.getBooleanValue(), equalTo(false));

    RoutingPolicy asPathGroupPolicy2 = c.getRoutingPolicies().get("AS_PATH_GROUP_POLICY2");
    result =
        asPathGroupPolicy2.call(
            envWithRoute(c, test.setAsPath(AsPath.ofSingletonAsSets(1L)).build()));
    assertThat(result.getBooleanValue(), equalTo(false));

    result =
        asPathGroupPolicy2.call(
            envWithRoute(c, test.setAsPath(AsPath.ofSingletonAsSets(2L)).build()));
    assertThat(result.getBooleanValue(), equalTo(true));
  }

  @Test
  public void testIfRouteExistMissingPrefix() throws IOException {
    String hostname = "juniper-missing-prefix-condition";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    assertThat(
        ccae.getWarnings().get(hostname).getFatalRedFlagWarnings(),
        contains(
            WarningMatchers.hasText(
                "FATAL: Missing route address for if-route-exists condition c0. Config will not"
                    + " pass commit checks."),
            WarningMatchers.hasText(
                "FATAL: Missing route address for if-route-exists condition c1. Config will not"
                    + " pass commit checks.")));
  }

  @Test
  public void testBgpPeerAsWarnings() throws IOException {
    String hostname = "bgp-peer-as-warnings";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    assertThat(
        ccae.getWarnings().get(hostname).getFatalRedFlagWarnings(),
        contains(
            WarningMatchers.hasText(
                "FATAL: Error in neighbor 192.0.2.2 of group EBGP_GROUP. External peer's AS (1111)"
                    + " must not be the same as the local AS (1111)."),
            WarningMatchers.hasText(
                "FATAL: Error in neighbor 192.0.2.3 of group EBGP_GROUP. Peer AS number must be"
                    + " configured for an external peer."),
            WarningMatchers.hasText(
                "FATAL: Error in neighbor 192.0.2.5 of group IBGP_GROUP. Internal peer's AS (2222)"
                    + " must be the same as local AS (1111)."),
            WarningMatchers.hasText(
                "FATAL: Error in neighbor 192.0.2.9 of group DEFAULT_GROUP. Peer AS number must be"
                    + " configured for an external peer.")));
  }

  @Test
  public void testSettingCommunityWithNoLiteralMemberHasFatalWarning() throws IOException {
    String hostname = "community-literals-warnings";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    assertThat(
        ccae.getWarnings().get(hostname).getFatalRedFlagWarnings(),
        containsInAnyOrder(
            WarningMatchers.hasText(
                "FATAL: 'COMMUNITY_WITH_NO_LITERAL' community contains no non-wildcard members in"
                    + " an add action"),
            WarningMatchers.hasText(
                "FATAL: 'COMMUNITY_WITH_NO_LITERAL' community contains no non-wildcard members in a"
                    + " set action")));
  }

  @Test
  public void testRiskyRegexWarnings() throws IOException {
    String hostname = "risky-regexes";
    Batfish batfish = getBatfishForConfigurationNames(hostname);

    List<ParseWarning> riskyParseWarnings =
        batfish
            .loadParseVendorConfigurationAnswerElement(batfish.getSnapshot())
            .getWarnings()
            .get("configs/" + hostname)
            .getRiskyParseWarnings();

    assertThat(
        riskyParseWarnings,
        containsInAnyOrder(
            hasComment(
                "RISK: Community regex 8075:[1][0][0-3,5-9][0-9][0-9]$ allows longer matches such"
                    + " as 18075:10000"),
            hasComment("RISK: Community string ':111' is interpreted as '0:111'")));
  }

  @Test
  public void testCommunityOverlapWarnings() throws IOException {
    String hostname = "overlapping-policy";
    Batfish batfish = getBatfishForConfigurationNames(hostname);

    List<ParseWarning> riskyParseWarnings =
        batfish
            .loadParseVendorConfigurationAnswerElement(batfish.getSnapshot())
            .getWarnings()
            .get("configs/" + hostname)
            .getRiskyParseWarnings();

    assertThat(
        riskyParseWarnings,
        containsInAnyOrder(hasComment("RISK: Overwriting existing then community set")));
  }

  @Test
  public void testPsFromExternal() {
    String hostname = "policy-statement-from-external";
    JuniperConfiguration juniperConfig = parseJuniperConfig(hostname);
    Configuration c = parseConfig(hostname);
    Warnings w = new Warnings();

    // Test policy statement with "from external type 1"
    PsTerm termType1 =
        juniperConfig
            .getMasterLogicalSystem()
            .getPolicyStatements()
            .get("test-external-type-1")
            .getTerms()
            .get("t1");
    assertNotNull(termType1);

    PsFromExternal fromExternalType1 = termType1.getFroms().getFromExternal();
    assertNotNull(fromExternalType1);
    assertThat(fromExternalType1.getType(), equalTo(OspfMetricType.E1));

    // Verify conversion to vendor-independent model
    assertThat(
        fromExternalType1.toBooleanExpr(juniperConfig, c, w),
        equalTo(new MatchProtocol(RoutingProtocol.OSPF_E1)));

    // Test policy statement with "from external type 2"
    PsTerm termType2 =
        juniperConfig
            .getMasterLogicalSystem()
            .getPolicyStatements()
            .get("test-external-type-2")
            .getTerms()
            .get("t1");
    assertNotNull(termType2);

    PsFromExternal fromExternalType2 = termType2.getFroms().getFromExternal();
    assertNotNull(fromExternalType2);
    assertThat(fromExternalType2.getType(), equalTo(OspfMetricType.E2));

    // Verify conversion to vendor-independent model
    assertThat(
        fromExternalType2.toBooleanExpr(juniperConfig, c, w),
        equalTo(new MatchProtocol(RoutingProtocol.OSPF_E2)));

    // Test policy statement with just "from external" (no type specified)
    PsTerm termAny =
        juniperConfig
            .getMasterLogicalSystem()
            .getPolicyStatements()
            .get("test-external-any")
            .getTerms()
            .get("t1");
    assertNotNull(termAny);

    PsFromExternal fromExternalAny = termAny.getFroms().getFromExternal();
    assertNotNull(fromExternalAny);
    assertThat(fromExternalAny.getType(), nullValue());

    // Verify conversion to vendor-independent model - should match any external type
    assertThat(
        fromExternalAny.toBooleanExpr(juniperConfig, c, w),
        equalTo(new MatchProtocol(RoutingProtocol.OSPF_E1, RoutingProtocol.OSPF_E2)));

    // Get the converted routing policies
    RoutingPolicy type1Policy = c.getRoutingPolicies().get("test-external-type-1");
    RoutingPolicy type2Policy = c.getRoutingPolicies().get("test-external-type-2");
    RoutingPolicy anyTypePolicy = c.getRoutingPolicies().get("test-external-any");
    assertNotNull(anyTypePolicy);

    // Create OSPF External Type 1 and Type 2 routes
    Prefix testPrefix = Prefix.parse("192.0.2.0/24");
    OspfExternalType1Route ospfE1Route =
        (OspfExternalType1Route)
            OspfExternalType1Route.testBuilder()
                .setNetwork(testPrefix)
                .setMetric(20)
                .setAdmin(100)
                .build();

    OspfExternalType2Route ospfE2Route =
        (OspfExternalType2Route)
            OspfExternalType2Route.builder()
                .setNetwork(testPrefix)
                .setNextHopIp(Ip.parse("10.0.0.1"))
                .setArea(0)
                .setMetric(20)
                .setCostToAdvertiser(10)
                .setLsaMetric(10)
                .setAdvertiser("advertiser")
                .setAdmin(100)
                .setNonRouting(true) // Required when using NextHopIp
                .build();

    assertTrue("Type 1 policy should match E1 route", type1Policy.processReadOnly(ospfE1Route));
    assertFalse(
        "Type 1 policy should not match E2 route", type1Policy.processReadOnly(ospfE2Route));

    assertTrue("Type 2 policy should match E2 route", type2Policy.processReadOnly(ospfE2Route));
    assertFalse(
        "Type 2 policy should not match E1 route", type2Policy.processReadOnly(ospfE1Route));

    // Test that "any type" policy matches both E1 and E2 routes
    assertTrue("Any type policy should match E1 route", anyTypePolicy.processReadOnly(ospfE1Route));
    assertTrue("Any type policy should match E2 route", anyTypePolicy.processReadOnly(ospfE2Route));
  }

  @Test
  public void testRiskyTerminalTermsInPolicyStatements() throws IOException {
    String hostname = "risky-terminal-actions";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    SortedSet<Warning> riskyWarnings = ccae.getWarnings().get(hostname).getRiskyRedFlagWarnings();

    // Test risky cases (should generate warnings)
    assertThat("Should have exactly 6 RISKY warnings", riskyWarnings, hasSize(6));

    String expectedRiskyRejectWarning =
        "RISK: 'policy-statement RISKY-REJECT term UNCONDITIONAL-REJECT then reject' always ends"
            + " processing, but there are 2 subsequent terms that will not be evaluated";
    assertThat(
        "Should have exact warning for RISKY-REJECT policy",
        riskyWarnings,
        hasItem(WarningMatchers.hasText(expectedRiskyRejectWarning)));

    String expectedRiskyAcceptWarning =
        "RISK: 'policy-statement RISKY-ACCEPT term UNCONDITIONAL-ACCEPT then accept' always ends"
            + " processing, but there is 1 subsequent term that will not be evaluated";
    assertThat(
        "Should have exact warning for RISKY-ACCEPT policy",
        riskyWarnings,
        hasItem(WarningMatchers.hasText(expectedRiskyAcceptWarning)));

    String expectedRiskyNextPolicyWarning =
        "RISK: 'policy-statement RISKY-NEXT-POLICY term UNCONDITIONAL-NEXT-POLICY then next policy'"
            + " always ends processing, but there is 1 subsequent term that will not be evaluated";
    assertThat(
        "Should have exact warning for RISKY-NEXT-POLICY policy",
        riskyWarnings,
        hasItem(WarningMatchers.hasText(expectedRiskyNextPolicyWarning)));

    String expectedTooManyTerminalWarning =
        "RISK: 'policy-statement TOO-MANY-TERMINAL term FIRST-UNCONDITIONAL-REJECT then reject'"
            + " always ends processing, but there are 2 subsequent terms that will not be"
            + " evaluated";
    assertThat(
        "Should have exact warning for TOO-MANY-TERMINAL policy",
        riskyWarnings,
        hasItem(WarningMatchers.hasText(expectedTooManyTerminalWarning)));

    String expectedTwoTerminalWarning =
        "RISK: 'policy-statement RISKY-TWO-TERMINAL term FIRST-UNCONDITIONAL-REJECT then reject'"
            + " always ends processing, but there are 2 subsequent terms that will not be"
            + " evaluated";
    assertThat(
        "Should have exact warning for RISKY-TWO-TERMINAL policy",
        riskyWarnings,
        hasItem(WarningMatchers.hasText(expectedTwoTerminalWarning)));

    String riskyMutatingWarning =
        "RISK: 'policy-statement RISKY-UNREACHABLE-MUTATING term UNCONDITIONAL-REJECT then reject'"
            + " always ends processing, but there is 1 subsequent term that will not be"
            + " evaluated";
    assertThat(
        "Should have exact warning for RISKY-UNREACHABLE-MUTATING policy",
        riskyWarnings,
        hasItem(WarningMatchers.hasText(riskyMutatingWarning)));
  }

  @Test
  public void testPsToConditionsFlag() throws IOException {
    String hostname = "risky-terminal-actions";
    JuniperConfiguration juniperConfig = parseJuniperConfig(hostname);

    // Test policy with only "to" condition
    PolicyStatement policyToOnly =
        juniperConfig.getMasterLogicalSystem().getPolicyStatements().get("TO-ONLY");
    PsTerm toConditionTerm = policyToOnly.getTerms().get("TO-CONDITION-TERM");

    assertThat(
        "Term with 'to' condition should have hasToConditions=true",
        toConditionTerm.hasAtLeastOneTo());
    assertThat(
        "Term with 'to' condition should not have any 'from' conditions",
        !toConditionTerm.hasAtLeastOneFrom());

    // Test policy with both "from" and "to" conditions
    PolicyStatement policyFromAndTo =
        juniperConfig.getMasterLogicalSystem().getPolicyStatements().get("FROM-AND-TO");
    PsTerm fromToConditionTerm = policyFromAndTo.getTerms().get("FROM-TO-CONDITION-TERM");

    assertThat(
        "Term with both 'from' and 'to' conditions should have hasToConditions=true",
        fromToConditionTerm.hasAtLeastOneTo());
    assertThat(
        "Term with both 'from' and 'to' conditions should have hasAtLeastOneFrom=true",
        fromToConditionTerm.hasAtLeastOneFrom());

    // Test policy with only "from" conditions (negative test case for hasToConditions)
    PolicyStatement policyFromOnly =
        juniperConfig.getMasterLogicalSystem().getPolicyStatements().get("SAFE-CONDITIONAL");
    PsTerm fromOnlyTerm = policyFromOnly.getTerms().get("CONDITIONAL-REJECT");

    assertThat(
        "Term with only 'from' conditions should have hasToConditions=false",
        !fromOnlyTerm.hasAtLeastOneTo());
    assertThat(
        "Term with only 'from' conditions should have hasAtLeastOneFrom=true",
        fromOnlyTerm.hasAtLeastOneFrom());
  }

  private final BddTestbed _b = new BddTestbed(ImmutableMap.of(), ImmutableMap.of());
}
