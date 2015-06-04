/*
 *  Copyright 2015 LG CNS.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); 
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 */

package scouter.test;

import java.util.Random;

import scouter.AnyTrace;
import scouter.agent.Configure;
import scouter.agent.LazyAgentBoot;
import scouter.agent.netio.request.net.RequestAgent;
import scouter.agent.trace.ProfileCollector;
import scouter.agent.trace.StringHashCache;
import scouter.agent.trace.TraceContext;
import scouter.agent.trace.TraceContextManager;
import scouter.agent.trace.TraceMain;
import scouter.io.DataOutputX;
import scouter.lang.pack.XLogPack;
import scouter.lang.step.MessageStep;
import scouter.util.CastUtil;
import scouter.util.DateUtil;
import scouter.util.IPUtil;
import scouter.util.KeyGen;
import scouter.util.ShellArg;
import scouter.util.SysJMX;
import scouter.util.ThreadUtil;

public class TpsRush {
	public static void main(String[] args) {

		ShellArg sh = new ShellArg(args);
		String server = sh.get("-h", "127.0.0.1");
		String port = sh.get("-p", "6100");

		int tps =CastUtil.cint(sh.get("-tps","1000"));
		String type = sh.get("-type", "tomcat");
		String name = sh.get("-name", "tomcat"+SysJMX.getProcessPID());
		System.setProperty("scouter_type", type);
		System.setProperty("scouter_name", name);
		
		System.setProperty("server.addr", server);
		System.setProperty("server.port", port);

		
		System.out.println("Scouter Test Simulation!!");
		System.out.println("  server = " + server + ":" +port);
		System.out.println("  tcp = " + tps);
		
		LazyAgentBoot.boot();
		RequestAgent.getInstance();

		double interval = 1000.0/tps;
		
		long now = System.currentTimeMillis();
		
		Random r = new Random();

		long txcount = 0 ;
		double tm = 0;
		long last_unit = 0;
		while (true) {
			txcount++;
			
			String serviceName = "service" + (next(r, 1000));
			int service_hash = StringHashCache.getUrlHash(serviceName);
			long txid = KeyGen.next();
			profile(txid,service_hash);
			long endtime = System.currentTimeMillis();
			
			
			int elapsed =next(r, 10000);
			int cpu = next(r, 10000);
			int sqlCount = next(r, 100);
			int sqlTime = next(r, 1000);
			String remoteAddr = IPUtil.toString(DataOutputX.toBytes(next(r,255)));
			String error = null;
			long visitor = KeyGen.next();
			
			
			
			XLogPack pack = TraceMain.txperf(endtime, txid, service_hash,serviceName, elapsed, cpu, sqlCount, sqlTime, remoteAddr, error, visitor);
			TraceMain.metering(pack);
			
		    long unit=endtime/5000;
		    if(last_unit!=unit){
		    	last_unit = unit;
		    	System.out.println(DateUtil.timestamp(endtime) + "  exe-tx=" + txcount+ "  " + Configure.getInstance().objName);
		    }
			tm = tm+interval;
			if(tm>1){
				ThreadUtil.sleep((int)tm);
			     tm = tm - ((int)tm);
			}
			long x = System.currentTimeMillis();
			if(x-now >1000000)
				break;
		}
		ThreadUtil.sleep(100000);
	}

	private static void profile(long txid, int serviceHash) {
		TraceContext ctx = new TraceContext(false);
		ctx.txid=txid;
		ctx.serviceHash=serviceHash;
		ctx.startTime=System.currentTimeMillis();
		
		long key = TraceContextManager.start(Thread.currentThread(), ctx);
		
		AnyTrace.message("profile 1");
		AnyTrace.message("profile 2");
		
		ctx.profile.close(true);
		TraceContextManager.end(key);
	}

	private static int next(Random r, int max) {
		return Math.abs(r.nextInt() % max);
	}
}