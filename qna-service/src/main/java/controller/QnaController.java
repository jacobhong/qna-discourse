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

    private static final String API_TOKEN = "xoxp-114414444772-115025525495-114722023077-ac9d157bf85e560d7681981988d4da64";
    @Resource
    private RestTemplate restTemplate;

    @RequestMapping(value = "/channels", method = RequestMethod.GET)
    public QnaResponse getChannels(@RequestParam(value = "text") String text) throws IOException {
        String url = "https://qnadiscourse.slack.com/api/channels.list?";
//        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        //https://qnadiscourse.slack.com/api/channels.history?
        // token=xoxp-114414444772-115025525495-113890578256-8fc0bc4b68d4ad40dd84f9cc61b2724d
        // channel=C3C7YK8TV
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

//        headers.add(X_API_KEY, getPropertyString(ADDRESS_TOKENIZER_MICROSERVICE_API_KEY));

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(url);

        // If we are updating
//        uriBuilder.pathSegment(token);

        uriBuilder.queryParam("token", API_TOKEN);
//        uriBuilder.queryParam("channel", text);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> channelsList = restTemplate.exchange(uriBuilder.build().encode().toUri(), HttpMethod.GET, entity, String.class);

        ObjectMapper mapper = new ObjectMapper();
        Map<String,Object> map = mapper.readValue(channelsList.getBody(), Map.class);

        String channelId = "";
        for(Map.Entry<String, Object> channels : map.entrySet())
        {
            if(channels.getKey().equals("channels"))
            {
//                channelId = (String) channels.getValue();
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
        logger.info(channelId);
//        logger.info("Entering addressResponse(channelsList={})", channelsList);
        logger.info("Entering map(map={})", map);

//        result = addressResponse.getBody();

//        logger.info(text);
//        String[] arr = text.split(",");


        logger.info("Exiting test(test={})");
        url = "https://qnadiscourse.slack.com/api/channels.history?";
        uriBuilder = UriComponentsBuilder.fromUriString(url);

        uriBuilder.queryParam("channel", channelId);
        uriBuilder.queryParam("token", API_TOKEN);

        ResponseEntity<String> addressResponse = restTemplate.exchange(uriBuilder.build().encode().toUri(), HttpMethod.GET, entity, String.class);
        logger.info("Entering addressResponse(addressResponse={})", addressResponse);
        QnaResponse response = new QnaResponse();
        response.setText(addressResponse.getBody());
        return response;
    }
}
