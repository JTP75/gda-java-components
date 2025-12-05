/**
 * 
 * This class is part of the Programming the Internet of Things
 * project, and is available via the MIT License, which can be
 * found in the LICENSE file at the top level of this repository.
 * 
 * Copyright (c) 2020 - 2025 by Andrew D. King
 */ 

package programmingtheiot.integration.connection;

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import programmingtheiot.common.DefaultDataMessageListener;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.AnthropicContentBlock;
import programmingtheiot.data.AnthropicMessage;
import programmingtheiot.data.AnthropicRole;
import programmingtheiot.data.DataUtil;
import programmingtheiot.data.ExecuteToolRequest;
import programmingtheiot.data.LLMHttpRequest;
import programmingtheiot.data.MessagesRequest;
import programmingtheiot.data.SystemStateData;
import programmingtheiot.gda.connection.*;

/**
 * This test case class contains very basic integration tests for
 * CoapClientConnector. It should not be considered complete,
 * but serve as a starting point for the student implementing
 * additional functionality within their Programming the IoT
 * environment.
 * 
 * NOTE: The CoAP server must be running before executing these tests.
 */
public class PuetceClientConnectorTest
{
	// static
	
	public static final int DEFAULT_TIMEOUT = 5;
	public static final boolean USE_DEFAULT_RESOURCES = true;
	
	private static final Logger _Logger =
		Logger.getLogger(PuetceClientConnectorTest.class.getName());
	
	// member var's
	
	private PuetceClientConnector puetceClient = null;
	private IDataMessageListener dataMsgListener = null;
	
	
	// test setup methods
	
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
	}
	
	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception
	{
	}
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception
	{
		this.puetceClient = new PuetceClientConnector();
		this.dataMsgListener = new DefaultDataMessageListener();
		
		this.puetceClient.setDataMessageListener(this.dataMsgListener);
	}
	
	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception
	{
	}
	
	// test methods
	
	/**
	 * 
	 */
	@Test
	public void testSendMessage()
	{
        Gson gson = new Gson();

		List<AnthropicContentBlock> content = new ArrayList<>();
		content.add(new AnthropicContentBlock.Text("Hello, whats your name?"));
        
		List<AnthropicMessage> messages = new ArrayList<>();
		messages.add(new AnthropicMessage(AnthropicRole.USER, content));

		// MessagesRequest request = new MessagesRequest(messages);

		// LLMHttpRequest req = new LLMHttpRequest(
		// 	"send_message", gson.toJsonTree(request).getAsJsonObject());

        // _Logger.info("req: " + gson.toJson(req));

        // assertTrue(puetceClient.sendPostRequest(
		// 	ResourceNameEnum.GDA_MESSAGE_PUETCE_RESOURCE, null, false, gson.toJson(req), 5));

		puetceClient.sendMessage(messages, "", false, 0.5f);

		try { Thread.sleep(5000L); } catch (Exception e) {}
	}

	/**
	 * 
	 */
	@Test
	public void testExecuteTool() 
	{
		puetceClient.executeTool("custom-motd-get_motd", new JsonObject());
		try { Thread.sleep(5000L); } catch (Exception e) {}
	}
	
	/**
	 * 
	 */
	@Test
	public void testSendMessageRawJson()
	{
        Gson gson = new Gson();
        String path = "/home/pacel/northeastern/TELE6530/piot/gda-java-components/src/test/java/programmingtheiot/integration/connection/test_request.json";
        String reqs = null;

        try {
            reqs = Files.readString(Paths.get(path));
        } catch (Exception e) {
            _Logger.severe(""+e);
            fail();
        }

        JsonObject req = gson.fromJson(reqs, JsonObject.class);

        _Logger.info("req: " + req);

        assertTrue(puetceClient.sendPostRequest(
			ResourceNameEnum.GDA_MESSAGE_PUETCE_RESOURCE, null, false, gson.toJson(req), 5));
	}
	
}
