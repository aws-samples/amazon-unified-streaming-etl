package com.amazonaws.samples.unified.streaming.etl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import software.amazon.awscdk.core.CfnParameter;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.dms.CfnEndpoint;
import software.amazon.awscdk.services.dms.CfnReplicationInstance;
import software.amazon.awscdk.services.dms.CfnReplicationSubnetGroup;
import software.amazon.awscdk.services.dms.CfnReplicationTask;
import software.amazon.awscdk.services.ec2.CfnInternetGateway;
import software.amazon.awscdk.services.ec2.CfnRoute;
import software.amazon.awscdk.services.ec2.CfnRouteTable;
import software.amazon.awscdk.services.ec2.CfnSecurityGroup;
import software.amazon.awscdk.services.ec2.CfnSecurityGroupProps;
import software.amazon.awscdk.services.ec2.CfnSubnet;
import software.amazon.awscdk.services.ec2.CfnSubnetRouteTableAssociation;
import software.amazon.awscdk.services.ec2.CfnVPC;
import software.amazon.awscdk.services.ec2.CfnVPCGatewayAttachment;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyDocument;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.rds.CfnDBInstance;
import software.amazon.awscdk.services.rds.CfnDBParameterGroup;
import software.amazon.awscdk.services.rds.CfnDBSecurityGroup;
import software.amazon.awscdk.services.rds.CfnDBSecurityGroupProps;
import software.amazon.awscdk.services.rds.CfnDBSubnetGroup;




public class UnifiedStreamETLDataStack extends Stack {

	public UnifiedStreamETLDataStack(final Construct parent, final String id) {
		this(parent, id, null);
	}
	
	public UnifiedStreamETLDataStack(final Construct parent, final String id, Map<String,Object> commonStackResMap) {
		this(parent, id, null, commonStackResMap);
	}

	public UnifiedStreamETLDataStack(final Construct parent, final String id, final StackProps props, Map<String,Object> commonStackResMap) {
		super(parent, id, props);
		
		//RDS input params
		
		CfnParameter vpcCidr = CfnParameter.Builder.create(this, "vpcCidr")
				        .type("String")
				        .description("VPC CIDR").defaultValue("20.0.0.0/16")
				        .build();
		
		CfnParameter snet1Cidr = CfnParameter.Builder.create(this, "snet1Cidr")
		        .type("String")
		        .description("Subnet1 CIDR").defaultValue("20.0.1.0/24")
		        .build();
		
		CfnParameter snet2Cidr = CfnParameter.Builder.create(this, "snet2Cidr")
		        .type("String")
		        .description("Subnet2 CIDR").defaultValue("20.0.2.0/24")
		        .build();
		
		CfnParameter sgCidr = CfnParameter.Builder.create(this, "sgCidr")
		        .type("String")
		        .description("SG CIDR").defaultValue("0.0.0.0/0")
		        .build();
			 
		
		CfnVPC.Builder vpcBldr = CfnVPC.Builder.create(this, "unified-vpc");
		vpcBldr.cidrBlock(vpcCidr.getValueAsString());
		vpcBldr.enableDnsHostnames(true);
		vpcBldr.enableDnsSupport(true);
		
		
		CfnVPC vpc = vpcBldr.build();
		
		CfnSubnet.Builder snetBldr = CfnSubnet.Builder.create(this, "unified-vpc-subnet1");
		snetBldr.availabilityZone("us-east-1a");
		snetBldr.cidrBlock(snet1Cidr.getValueAsString());
		snetBldr.mapPublicIpOnLaunch(true);
		snetBldr.vpcId(vpc.getRef());
		CfnSubnet snet1 = snetBldr.build();
		snet1.addDependsOn(vpc);
		
		CfnSubnet.Builder snetBldr2 = CfnSubnet.Builder.create(this, "unified-vpc-subnet2");
		snetBldr2.availabilityZone("us-east-1b");
		snetBldr2.cidrBlock(snet2Cidr.getValueAsString());
		snetBldr2.mapPublicIpOnLaunch(true);
		snetBldr2.vpcId(vpc.getRef());
		CfnSubnet snet2 = snetBldr2.build();
		snet2.addDependsOn(vpc);
		
		CfnParameter dbName = CfnParameter.Builder.create(this, "dbName")
		        .type("String")
		        .description("DB Name").defaultValue("uordersdb")
		        .build();
		
		CfnParameter dbUser = CfnParameter.Builder.create(this, "dbUser")
		        .type("String")
		        .description("DB Name").defaultValue("uordersuser")
		        .build();
		
		CfnParameter dbPass = CfnParameter.Builder.create(this, "dbPass")
		        .type("String")
		        .description("DB Password").defaultValue("uorderspass")
		        .build();

		CfnParameter dbInstType = CfnParameter.Builder.create(this, "dbInsttype")
		        .type("String")
		        .description("DB Instance Type").defaultValue("db.t3.large")
		        .build();
		
		CfnParameter dmsInstType = CfnParameter.Builder.create(this, "dmsInsttype")
		        .type("String")
		        .description("DB Instance Type").defaultValue("dms.t2.medium")
		        .build();
		
			
		
		CfnSecurityGroupProps.Builder sgpBuilder =  new CfnSecurityGroupProps.Builder();
		sgpBuilder.vpcId(vpc.getRef());
		sgpBuilder.groupName("UnifiedSG");
		sgpBuilder.groupDescription("MySQL-OrderDB SG");
		
		
		
		CfnSecurityGroup sg = new CfnSecurityGroup(this,"vpcsecgrp", sgpBuilder.build());
		
		sg.addDependsOn(vpc);
		
		CfnSecurityGroup.IngressProperty.Builder sgiBldr = CfnSecurityGroup.IngressProperty.builder();
		sgiBldr.fromPort(3306);
		sgiBldr.toPort(3306);
		sgiBldr.ipProtocol("TCP");
		sgiBldr.cidrIp(sgCidr.getValueAsString());
		
		List<Object> sgiLst = new ArrayList<Object>();
		sgiLst.add(sgiBldr.build());
		
		sg.setSecurityGroupIngress(sgiLst);
		sg.setVpcId(vpc.getRef());
		
			
		CfnInternetGateway.Builder igwBldr = CfnInternetGateway.Builder.create(this, "igwBldr");
		CfnInternetGateway igw = igwBldr.build();
		
		CfnVPCGatewayAttachment.Builder gwatchBldr = CfnVPCGatewayAttachment.Builder.create(this, "gwatchBldr");
		
		gwatchBldr.internetGatewayId(igw.getRef());
		gwatchBldr.vpcId(vpc.getRef());
		
		CfnVPCGatewayAttachment gwatch = gwatchBldr.build();
		
		gwatch.addDependsOn(vpc);
		gwatch.addDependsOn(igw);
		
		//create route table 
		CfnRouteTable rt = CfnRouteTable.Builder.create(this, "rtBldr")
							.vpcId(vpc.getRef()).build();
	
		//build route in  route table for a route out to internet
		
		CfnRoute rte = CfnRoute.Builder.create(this, "rBldr")
							.gatewayId(igw.getRef())
							.routeTableId(rt.getRef())
							.destinationCidrBlock("0.0.0.0/0").build();	
		
		rte.addDependsOn(rt);
		
		CfnSubnetRouteTableAssociation rta1 = CfnSubnetRouteTableAssociation.Builder.create(this,"rtaBldr1")
													.routeTableId(rt.getRef())
													.subnetId(snet1.getRef()).build();
													
		rta1.addDependsOn(rt);
		rta1.addDependsOn(snet1);
		
		CfnSubnetRouteTableAssociation rta2 = CfnSubnetRouteTableAssociation.Builder.create(this,"rtaBldr2")
				.routeTableId(rt.getRef())
				.subnetId(snet2.getRef()).build();
				
		rta2.addDependsOn(rt);
		rta2.addDependsOn(snet2);
		
		
		//setup db sec group
		
		CfnDBSecurityGroupProps.Builder dsgpBuilder =  new CfnDBSecurityGroupProps.Builder();
		dsgpBuilder.ec2VpcId(vpc.getRef());
		//dsgpBuilder.groupName("UnifiedSG");
		dsgpBuilder.groupDescription("MySQL-OrderDB DSG");
		
		CfnDBSecurityGroup.IngressProperty.Builder dsgiBldr = CfnDBSecurityGroup.IngressProperty.builder();
		
		//dsgiBldr.ec2SecurityGroupId(sg.getRef());
		dsgiBldr.cidrip(sgCidr.getValueAsString());
		
		
		List<Object> dsgiLst = new ArrayList<Object>();
		dsgiLst.add(dsgiBldr.build());
		
		dsgpBuilder.dbSecurityGroupIngress(dsgiLst);
		
		
		
		CfnDBSecurityGroup dsg = new CfnDBSecurityGroup(this,"dbsecgrp", dsgpBuilder.build());
		
		
		//dsg.setDbSecurityGroupIngress(sgiLst);
		dsg.addDependsOn(vpc);
		//dsg.setEc2VpcId(vpc.getRef());
		//dsg.setDbSecurityGroupIngress(dsgiLst);
		
		
		
		//setup rds mysql instance
		
		CfnDBInstance.Builder dbInstBuilder = CfnDBInstance.Builder.create(this, "MySQL-OrderDB");
		
		
		dbInstBuilder.engine("MySQL");
		dbInstBuilder.engineVersion("5.7.22");
		
		dbInstBuilder.dbInstanceClass(dbInstType.getValueAsString());
		
		dbInstBuilder.dbName(dbName.getValueAsString());
		dbInstBuilder.dbInstanceIdentifier(dbName.getValueAsString());
		dbInstBuilder.masterUsername(dbUser.getValueAsString());
		
		//SecretValue masterUserPassword = SecretValue.plainText(dbPass.getValueAsString());
		dbInstBuilder.masterUserPassword(dbPass.getValueAsString());
		dbInstBuilder.multiAz(false);
		dbInstBuilder.deletionProtection(false);
		dbInstBuilder.allocatedStorage("50");
		dbInstBuilder.publiclyAccessible(true);
		
		List<String> sgList = new ArrayList<String>();
		sgList.add(sg.getRef());
		
		dbInstBuilder.vpcSecurityGroups(sgList);
		dbInstBuilder.enablePerformanceInsights(true);
		
		List<String> dsgList = new ArrayList<String>();
		dsgList.add(dsg.getRef());
		dbInstBuilder.dbSecurityGroups(dsgList);
		
		//create db subnet group
		CfnDBSubnetGroup.Builder dbsgrpBldr = CfnDBSubnetGroup.Builder.create(this, "dbsgrpBldr");
		dbsgrpBldr.dbSubnetGroupName("DB-Subnet-Grp");
		dbsgrpBldr.dbSubnetGroupDescription("db subnet group");
		
		List<String> dbsngList = new ArrayList<String>();
		dbsngList.add(snet1.getRef());
		dbsngList.add(snet2.getRef());
		
		dbsgrpBldr.subnetIds(dbsngList);
		
		CfnDBSubnetGroup dbsng = dbsgrpBldr.build();
		
		dbInstBuilder.dbSubnetGroupName(dbsng.getDbSubnetGroupName());
		
		//db param group
		
		CfnDBParameterGroup.Builder dbparmBldr = CfnDBParameterGroup.Builder.create(this, "MySQL-ParmGrp-Id");
		
		dbparmBldr.family("mysql5.7");
		dbparmBldr.description("mysql param group for cdc");
		Map<String, String> dbparmsMap =  new HashMap<String,String>();
		dbparmsMap.put("binlog_format", "ROW");
		dbparmsMap.put("binlog_checksum", "NONE");
		
		dbparmBldr.parameters(dbparmsMap);
		dbInstBuilder.dbParameterGroupName(dbparmBldr.build().getRef());
		
		
		//build db
		CfnDBInstance dbInst = dbInstBuilder.build();
		dbInst.addDependsOn(rta2);
		
		
		//create DMS stack 
		
		//create dms repl instance
		
		CfnReplicationInstance.Builder riBldr =  CfnReplicationInstance.Builder.create(this, "riBldr");
		
		riBldr.replicationInstanceIdentifier("MySQL-rpl-Id");
		riBldr.engineVersion("3.3");
		riBldr.multiAz(false);
		riBldr.publiclyAccessible(true);
		
		CfnReplicationSubnetGroup.Builder rsgBldr = CfnReplicationSubnetGroup.Builder.create(this, "repl-subnet-grp-id");
		rsgBldr.replicationSubnetGroupIdentifier("UnifiedReplSubnetGrp");
		rsgBldr.replicationSubnetGroupDescription("unified replication subnet group");
		
		List<String> rsgList = new ArrayList<String>();
		rsgList.add(snet1.getRef());
		rsgList.add(snet2.getRef());
		
		
		rsgBldr.subnetIds(rsgList);
		
		
		CfnReplicationSubnetGroup rsgSnetGrp = rsgBldr.build(); 
		rsgSnetGrp.addDependsOn(vpc);
		rsgSnetGrp.addDependsOn(snet1);
		rsgSnetGrp.addDependsOn(snet2);
		
		riBldr.replicationSubnetGroupIdentifier(rsgSnetGrp.getRef());
		
		
		List<String> sgListIds = new ArrayList<String>();
		sgListIds.add(sg.getRef());
		riBldr.vpcSecurityGroupIds(sgListIds);
		riBldr.replicationInstanceClass(dmsInstType.getValueAsString());
		
		CfnReplicationInstance ri = riBldr.build();
		ri.setPubliclyAccessible(true);
		ri.addDependsOn(vpc);
		

		//create dms source endpoint
		
		CfnEndpoint.Builder epBldr = CfnEndpoint.Builder.create(this, "MySQL-DMS-SRC-EP");
		epBldr.endpointIdentifier("MySQLEndPointId");
		epBldr.endpointType("source");
		epBldr.engineName("mysql");
		epBldr.serverName(dbInst.getAttrEndpointAddress());
		epBldr.username(dbUser.getValueAsString());
		epBldr.password(dbPass.getValueAsString());
		epBldr.port(3306);
		
		CfnEndpoint srcEp  = epBldr.build();
		
		srcEp.addDependsOn(ri);
		
		//create dms target endpoint
		
		CfnEndpoint.Builder epBldr2 = CfnEndpoint.Builder.create(this, "Kinesis-DMS-TGT-EP");
		epBldr2.endpointIdentifier("KinesisEndPointId");
		epBldr2.endpointType("target");
		epBldr2.engineName("kinesis");
		
		
		//TODO fix streamArn and role settings with param values
		
		//setup iam role and policies
		
		List<String> actionList = new ArrayList<String>();
		
		List<String> resourceList = new ArrayList<String>();
		
		PolicyStatement.Builder policyStmt = PolicyStatement.Builder.create();
		
		List<PolicyStatement> policyStmtList = new ArrayList<PolicyStatement>();
		
		actionList = new ArrayList<String>();
		actionList.add("kinesis:DescribeStream");
		actionList.add("kinesis:PutRecord");
		actionList.add("kinesis:PutRecords");
		
		resourceList = new ArrayList<String>();
		resourceList.add((String)commonStackResMap.get("unifiedOrderStreamArn"));
		
		policyStmt = PolicyStatement.Builder.create();
		policyStmt.sid("WriteKinesis");
		policyStmt.effect(Effect.ALLOW);
		policyStmt.actions(actionList);
		policyStmt.resources(resourceList);
		
		policyStmtList.add(policyStmt.build());
		
		
		PolicyDocument.Builder policyDoc = PolicyDocument.Builder.create();
		policyDoc.statements(policyStmtList);
		
		
		HashMap<String, PolicyDocument> policyMap = new HashMap<String, PolicyDocument>();
		policyMap.put("UnifiedETLDmsKinesisPolicy", policyDoc.build());
		
		
		Role UnifiedETLDmsKinesisRole =
		        Role.Builder.create(this, "UnifiedETLDmsKinesisRole")
		            .assumedBy(new ServicePrincipal("dms.amazonaws.com"))
		            .inlinePolicies(policyMap)
		            .build();
		
		CfnEndpoint.KinesisSettingsProperty.Builder ksBldr = new CfnEndpoint.KinesisSettingsProperty.Builder();
		ksBldr.streamArn((String)commonStackResMap.get("unifiedOrderStreamArn"));
		ksBldr.serviceAccessRoleArn(UnifiedETLDmsKinesisRole.getRoleArn());
		ksBldr.messageFormat("json");
				
		epBldr2.kinesisSettings(ksBldr.build());
		CfnEndpoint tgtEp = epBldr2.build();
		
		tgtEp.addDependsOn(ri);
		
		
		CfnReplicationTask.Builder rtBldr = CfnReplicationTask.Builder.create(this, "DMS-Repl-Task-Id");
		rtBldr.replicationInstanceArn(ri.getRef());
		rtBldr.replicationTaskIdentifier("DMS-Repl-Task-Orders");
		rtBldr.sourceEndpointArn(srcEp.getRef());
		rtBldr.targetEndpointArn(tgtEp.getRef());
		rtBldr.migrationType("cdc");
		
		rtBldr.tableMappings("{\n" + 
				"    \"rules\": [\n" + 
				"        {\n" + 
				"            \"rule-type\": \"selection\",\n" + 
				"            \"rule-id\": \"1\",\n" + 
				"            \"rule-name\": \"1\",\n" + 
				"            \"rule-action\": \"include\",\n" + 
				"            \"object-locator\": {\n" + 
				"                \"schema-name\": \"orders\",\n" + 
				"                \"table-name\": \"%\"\n" + 
				"            }\n" + 
				"        },\n" + 
				"        {\n" + 
				"            \"rule-type\": \"object-mapping\",\n" + 
				"            \"rule-id\": \"2\",\n" + 
				"            \"rule-name\": \"2\",\n" + 
				"            \"rule-action\": \"map-record-to-record\",\n" + 
				"            \"target-table-name\": \"order\",\n" + 
				"            \"object-locator\": {\n" + 
				"                \"schema-name\": \"orders\",\n" + 
				"                \"table-name\": \"order\"\n" + 
				"            },\n" + 
				"            \"mapping-parameters\": {\n" + 
				"                \"partition-key-type\": \"schema-table\"\n" + 
				"            }\n" + 
				"        },\n" + 
				"        {\n" + 
				"            \"rule-type\": \"object-mapping\",\n" + 
				"            \"rule-id\": \"3\",\n" + 
				"            \"rule-name\": \"3\",\n" + 
				"            \"rule-action\": \"map-record-to-record\",\n" + 
				"            \"target-table-name\": \"order_item\",\n" + 
				"            \"object-locator\": {\n" + 
				"                \"schema-name\": \"orders\",\n" + 
				"                \"table-name\": \"order_item\"\n" + 
				"            },\n" + 
				"            \"mapping-parameters\": {\n" + 
				"                \"partition-key-type\": \"schema-table\"\n" + 
				"            }\n" + 
				"        }\n" + 
				"    ]\n" + 
				"}");
		
		CfnReplicationTask rplTsk = rtBldr.build();
		
		rplTsk.addDependsOn(srcEp);
		rplTsk.addDependsOn(tgtEp);
		
		
		
		
	
		
		
		
		
		
		
		
		
		
		
		
		
			
		
	}
	
}