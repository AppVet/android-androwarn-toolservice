A tool adapter is a tool configuration file that resides on the AppVet 
server to allow AppVet to integrate with the (possibly remote) tool service.

To create a new Android tool adapter, rename the example tool adapter
"example-service.xml" and 
copy the renamed file into 
$APPVET_FILES_HOME/conf/tool_adapters/android on the AppVet server. 

Also, edit .settings/org.eclipse.wst.common.component and change all
occurrences of "example-service" to your service project name (e.g.,
"android-androwarn-toolservice").

To ensure conformance to the AppVet tool adapter specification as defined
by the AppVet ToolAdapter.xsd schema file, first generate the tool adapter
XML file from the XSD file and edit the XML file to support your specific tool.

To link an existing .xml file with ToolAdapter.xsd, right-click on ToolAdapter.xsd
and select Generate > XML File > Advanced > Link To File In System and
select the .xml file.
