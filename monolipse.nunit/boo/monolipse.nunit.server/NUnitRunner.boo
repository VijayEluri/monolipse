﻿namespace monolipse.nunit.server

import System
import System.IO
import NUnit.Core from nunit.core
import NUnit.Core from nunit.core.interfaces as NUnitInterfaces
import monolipse.core

class NUnitProxy(System.MarshalByRefObject):
	def Run(assemblyName as string, client as ProcessMessengerClient):
		CoreExtensions.Host.InitializeService();
		AppDomain.CurrentDomain.AssemblyResolve += RelativeAssemblyResolver(Path.GetDirectoryName(assemblyName)).AssemblyResolve
		pkg = NUnitInterfaces.TestPackage(Path.GetFileName(assemblyName), [assemblyName])
		suite = TestSuiteBuilder().Build(pkg)
		client.Send(Message(Name: "TESTS-STARTED", Payload: suite.TestCount.ToString())) 
		suite.Run(TestListener(client))

service NUnitRunner:
	
	onMessage "RUN":
		try:
			arguments = /,/.Split(message.Payload.Trim())
			
			assemblyName = arguments[0]
#			testCases = arguments[1:]
			
			using domain = AppDomain.CreateDomain("Tests"):
				proxy as NUnitProxy = domain.CreateInstanceAndUnwrap(typeof(NUnitProxy).Assembly.ToString(), typeof(NUnitProxy).FullName)
				proxy.Run(assemblyName, client)
			
		except x:
			Console.Error.WriteLine(x)
		ensure:
			send "TESTS-FINISHED"
			(client as NetworkProcessMessengerClient).Stop()

