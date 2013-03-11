package edu.mayo.mprc.sge;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import org.testng.Assert;
import org.testng.annotations.Test;

public final class SgePacketTest {

	@Test
	public static void shouldDeserializeXTandem() {
		final String xml = "<edu.mayo.mprc.sge.SgePacket>\n" +
				"  <workPacket class=\"edu.mayo.mprc.xtandem.XTandemWorkPacket\">\n" +
				"    <tokenMap>\n" +
				"      <entry>\n" +
				"        <edu.mayo.mprc.daemon.files.FieldIndex>\n" +
				"          <field>workFolder</field>\n" +
				"        </edu.mayo.mprc.daemon.files.FieldIndex>\n" +
				"        <edu.mayo.mprc.daemon.files.FileTokenFactory_-SharedToken>\n" +
				"          <sourceDaemonConfigInfo>\n" +
				"            <daemonId>Node021</daemonId>\n" +
				"            <sharedFileSpacePath>/mnt/raid1/</sharedFileSpacePath>\n" +
				"          </sourceDaemonConfigInfo>\n" +
				"          <tokenPath>shared:/software/swift/swift_2.5.6/var/cache/tandem/wip/wip85136348311630908/</tokenPath>\n" +
				"        </edu.mayo.mprc.daemon.files.FileTokenFactory_-SharedToken>\n" +
				"      </entry>\n" +
				"      <entry>\n" +
				"        <edu.mayo.mprc.daemon.files.FieldIndex>\n" +
				"          <field>outputFile</field>\n" +
				"        </edu.mayo.mprc.daemon.files.FieldIndex>\n" +
				"        <edu.mayo.mprc.daemon.files.FileTokenFactory_-SharedToken>\n" +
				"          <sourceDaemonConfigInfo reference=\"../../../entry/edu.mayo.mprc.daemon.files.FileTokenFactory_-SharedToken/sourceDaemonConfigInfo\"/>\n" +
				"          <tokenPath>shared:/software/swift/swift_2.5.6/var/cache/tandem/wip/wip85136348311630908/ch_o261_072711_04_CRuby_2.xml</tokenPath>\n" +
				"        </edu.mayo.mprc.daemon.files.FileTokenFactory_-SharedToken>\n" +
				"      </entry>\n" +
				"      <entry>\n" +
				"        <edu.mayo.mprc.daemon.files.FieldIndex>\n" +
				"          <field>inputFile</field>\n" +
				"        </edu.mayo.mprc.daemon.files.FieldIndex>\n" +
				"        <edu.mayo.mprc.daemon.files.FileTokenFactory_-SharedToken>\n" +
				"          <sourceDaemonConfigInfo reference=\"../../../entry/edu.mayo.mprc.daemon.files.FileTokenFactory_-SharedToken/sourceDaemonConfigInfo\"/>\n" +
				"          <tokenPath>shared:/software/swift/swift_2.5.6/var/cache/mgf/08/fd/cc/b1/1/ch_o261_072711_04_CRuby_2.mgf</tokenPath>\n" +
				"        </edu.mayo.mprc.daemon.files.FileTokenFactory_-SharedToken>\n" +
				"      </entry>\n" +
				"      <entry>\n" +
				"        <edu.mayo.mprc.daemon.files.FieldIndex>\n" +
				"          <field>searchParamsFile</field>\n" +
				"        </edu.mayo.mprc.daemon.files.FieldIndex>\n" +
				"        <edu.mayo.mprc.daemon.files.FileTokenFactory_-SharedToken>\n" +
				"          <sourceDaemonConfigInfo reference=\"../../../entry/edu.mayo.mprc.daemon.files.FileTokenFactory_-SharedToken/sourceDaemonConfigInfo\"/>\n" +
				"          <tokenPath>shared:/projects/ch_C_Ruby/July_2011_iTraq/sw_072911_CRuby_charge_wo_mods/params/tandem.xml.template</tokenPath>\n" +
				"        </edu.mayo.mprc.daemon.files.FileTokenFactory_-SharedToken>\n" +
				"      </entry>\n" +
				"      <entry>\n" +
				"        <edu.mayo.mprc.daemon.files.FieldIndex>\n" +
				"          <field>databaseFile</field>\n" +
				"        </edu.mayo.mprc.daemon.files.FieldIndex>\n" +
				"        <edu.mayo.mprc.daemon.files.FileTokenFactory_-SharedToken>\n" +
				"          <sourceDaemonConfigInfo reference=\"../../../entry/edu.mayo.mprc.daemon.files.FileTokenFactory_-SharedToken/sourceDaemonConfigInfo\"/>\n" +
				"          <tokenPath>shared:/databases/SPmous081031.fasta</tokenPath>\n" +
				"        </edu.mayo.mprc.daemon.files.FileTokenFactory_-SharedToken>\n" +
				"      </entry>\n" +
				"    </tokenMap>\n" +
				"    <taskId>sw_072911_CRuby_charge_wo_mods.Tandem_search:22</taskId>\n" +
				"    <outputFile>/mnt/raid1/software/swift/swift_2.5.6/var/cache/tandem/wip/wip85136348311630908/ch_o261_072711_04_CRuby_2.xml</outputFile>\n" +
				"    <searchParamsFile>/mnt/raid1/projects/ch_C_Ruby/July_2011_iTraq/sw_072911_CRuby_charge_wo_mods/params/tandem.xml.template</searchParamsFile>\n" +
				"    <databaseFile>/mnt/raid1/databases/SPmous081031.fasta</databaseFile>\n" +
				"    <inputFile>/mnt/raid1/software/swift/swift_2.5.6/var/cache/mgf/08/fd/cc/b1/1/ch_o261_072711_04_CRuby_2.mgf</inputFile>\n" +
				"    <publishResultFiles>false</publishResultFiles>\n" +
				"    <workFolder>/mnt/raid1/software/swift/swift_2.5.6/var/cache/tandem/wip/wip85136348311630908</workFolder>\n" +
				"  </workPacket>\n" +
				"  <messengerInfo>\n" +
//				"    <registryInfo serialization=\"custom\">\n" +
//				"      <java.net.InetSocketAddress>\n" +
//				"        <default>\n" +
//				"          <port>41471</port>\n" +
//				"          <addr class=\"java.net.Inet4Address\" resolves-to=\"java.net.InetAddress\">\n" +
//				"            <hostName>node021.mprc.mayo.edu</hostName>\n" +
//				"            <address>-1407716459</address>\n" +
//				"            <family>2</family>\n" +
//				"          </addr>\n" +
//				"        </default>\n" +
//				"      </java.net.InetSocketAddress>\n" +
//				"    </registryInfo>\n" +
				"    <messengerRemoteName>edu.mayo.mprc.messaging.rmi.SimpleOneWayMessenger9</messengerRemoteName>\n" +
				"  </messengerInfo>\n" +
				"  <workerFactoryConfig class=\"edu.mayo.mprc.xtandem.XTandemWorker$Config\">\n" +
				"    <tandemExecutable>tandem.exe</tandemExecutable>\n" +
				"  </workerFactoryConfig>\n" +
				"  <daemonConfigInfo>\n" +
				"    <daemonId>Node021</daemonId>\n" +
				"    <sharedFileSpacePath>/mnt/raid1/</sharedFileSpacePath>\n" +
				"  </daemonConfigInfo>\n" +
				"  <fileSharingFactoryURI serialization=\"custom\">\n" +
				"    <java.net.URI>\n" +
				"      <default>\n" +
				"        <string>failover:(tcp://node021.mprc.mayo.edu:61617?wireFormat.maxInactivityDuration=0)</string>\n" +
				"      </default>\n" +
				"    </java.net.URI>\n" +
				"  </fileSharingFactoryURI>\n" +
				"  <sharedTempDirectory>/mnt/raid1/software/swift/swift_2.5.6/var/tmp</sharedTempDirectory>\n" +
				"</edu.mayo.mprc.sge.SgePacket>";
		final XStream xStream = new XStream(new DomDriver());
		final Object o = xStream.fromXML(xml);
		Assert.assertNotNull(o);
	}
}
