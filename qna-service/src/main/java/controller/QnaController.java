package controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import config.QnaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jacobhong on 12/8/16.
 */
@RestController
@RequestMapping(value = "/qna-discourse", produces = MediaType.APPLICATION_JSON_VALUE)
public class QnaController
{
    private static final Logger logger = LoggerFactory.getLogger(QnaController.class);

    private static final String API_TOKEN = "xoxp-114414444772-115025525495-115320773654-a57c65203f31524b485464e0191275fb";
    @Resource
    private RestTemplate restTemplate;

    @RequestMapping(value = "/channels", method = RequestMethod.GET)
    public QnaResponse getChannels(@RequestParam(value = "text") String text) throws IOException {
        String url = "https://qnadiscourse.slack.com/api/channels.list?";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(url);

        uriBuilder.queryParam("token", API_TOKEN);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> channelsList = restTemplate.exchange(uriBuilder.build().encode().toUri(), HttpMethod.GET, entity, String.class);

        ObjectMapper mapper = new ObjectMapper();
        Map<String,Object> map = mapper.readValue(channelsList.getBody(), Map.class);

        String channelId = "";
        for(Map.Entry<String, Object> channels : map.entrySet())
        {
            if(channels.getKey().equals("channels"))
            {
                List<Map<String, String>> channelArray = (List<Map<String, String>>) channels.getValue();
                for(Map c : channelArray)
                {
                    if(c.get("name").equals(text.replaceAll("\"", "")))
                    {
                        channelId = (String) c.get("id");
                    }
                }
            }
        }
        logger.info("Entering map(map={})", map);

        url = "https://qnadiscourse.slack.com/api/channels.history?";
        uriBuilder = UriComponentsBuilder.fromUriString(url);

        uriBuilder.queryParam("channel", channelId);
        uriBuilder.queryParam("token", API_TOKEN);

        ResponseEntity<String> addressResponse = restTemplate.exchange(uriBuilder.build().encode().toUri(), HttpMethod.GET, entity, String.class);
        map = mapper.readValue(addressResponse.getBody(), Map.class);

        /**
         * logic to fetch messages between emojis
         */
//        Map<String, List<String>> messagesResponse = new LinkedHashMap<>();
        List<List<String>> msgResponse = new ArrayList<>();
        boolean reactionFound = false;
        boolean end = false;
        for(Map.Entry<String, Object> messages : map.entrySet())
        {
            if(messages.getKey().equals("messages"))
            {
                List<Map<String, String>> messagesArray = (List<Map<String, String>>) messages.getValue();
                for(Map c : messagesArray)
                {
                    if(!end)
                    {
                        String usr = (String) c.get("user");
                        String txt = (String) c.get("text");
                        if(c.containsKey("reactions"))
                        {
                            List<LinkedHashMap<String, Object>> reactions = (List<LinkedHashMap<String, Object>>) c.get("reactions");
                            for(LinkedHashMap<String, Object> r : reactions)
                            {
                                if(r.get("name").equals("star2"))
                                {
                                    msgResponse.add(0, new ArrayList(){{add(usr);add(txt);}});
                                    reactionFound = true;
                                }
                                if(r.get("name").equals("star"))
                                {
                                    msgResponse.add(0, new ArrayList(){{add(usr);add(txt);}});
                                    end = true;
                                }
                            }
                        }
                        else if(reactionFound)
                        {
                            msgResponse.add(0, new ArrayList(){{add(usr);add(txt);}});
                        }
                    }
                }
            }
        }
        logger.info("Entering messagesResponse(messagesResponse={})", msgResponse);
        QnaResponse response = new QnaResponse();
        response.setText(String.valueOf(msgResponse));
        return response;
    }
}
