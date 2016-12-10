package controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import config.QnaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
 * Questions n Answers for Discourse, via slack....
 *
 *
 * This controller facilitates communication between slack and discourse
 *
 * Created by jacobhong on 12/8/16.
 */
@RestController
@RequestMapping(value = "/qna-discourse", produces = MediaType.APPLICATION_JSON_VALUE)
public class QnaController
{
    private static final Logger logger = LoggerFactory.getLogger(QnaController.class);

    private static final String BASE_SLACK_URL = "https://qnadiscourse.slack.com/api";
    private static final String BASE_DISCOURSE_URL = "http://discourse.chrometime.com";
    private static final String SLACK_API_TOKEN = "xoxp-114414444772-115025525495-";
    private static final String SLACK_API_TOKEN2 = "115710857318-be725a35dfa55589619922d610e4e4fe";
    private static final String CATEGORIES_URL = BASE_DISCOURSE_URL + "/categories.json?";
    private static final String CHANNELS_URL = BASE_SLACK_URL + "/channels.list?";
    private static final String CHANNELS_HISTORY_URL = BASE_SLACK_URL + "/channels.history?";
    private static final String USERS_NAME_URL = BASE_SLACK_URL + "/users.list?";
    private static final String DISCOURSE_TOPIC_URL = BASE_DISCOURSE_URL + "/posts?";
    private static final String DISCOURSE_API_KEY = "4e188b541f7fa868334dced411e5bf453dddf73c6f255558a212d6a7e37339da";
    private static final String DISCOURSE_USR = "jacob.hong";
    private static final String DISCOURSE_RESPONSE_URL = "http://discourse.chrometime.com/t/";


    // TODO : add ability to create categories if not exist(right now unknown categories are ignored)
    // TODO : add search capability by keyword/category directly into slack
    // TODO : possibly remove emojis after topic created, or create bot to do so based on events
    // TODO : generate legit url after creating topic(right now its fake)
    // TODO : grab images from posts
    // TODO : possibly create threads from pinned messages
    // TODO : make code cleaner and use transfer objects instead of maps
    // TODO : create http util


    @Resource
    private RestTemplate restTemplate;

    /**
     * Create a topic to CR-Discourse by fetching messages from slack that have been
     * marked with specific emojis
     *
     * @param text
     * @return
     * @throws IOException
     */
    @RequestMapping(value = "/topic", method = RequestMethod.POST)
    public QnaResponse createDiscourseTopic(@RequestParam(value = "text") String text) throws IOException {
        /**
         * First call slack to get list of channels, in order to get channel id
         */
        // 0 = channel name, 1 = topic name, 2 = category
        String[] textParams = text.split("\\|");
        String channelId = getChannelId(textParams[0]);
        /**
         * Fetch list of users because we need to map user id > user name
         */
        List<Map<String, Object>> users = getUsers();
        /**
         * Fetch all channel history to find messages between emojis
         */
        String messages = getChannelsHistory(channelId, users);
        /**
         * Send results to discourse to create thread
         */
        String url = createTopic(textParams[1], messages, textParams[2]);

        QnaResponse qnaResponse = new QnaResponse();
        qnaResponse.setText(url);
        logger.info("Successfully created topic... \n" + url);

        return qnaResponse;
    }

    @RequestMapping(value = "/categories", method = RequestMethod.GET)
    public String getCategories() throws IOException {
        logger.info("Fetching categories...");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(CATEGORIES_URL);

        uriBuilder.queryParam("api_key", DISCOURSE_API_KEY);
        uriBuilder.queryParam("api_username", DISCOURSE_USR);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(uriBuilder.build().encode().toUri(), HttpMethod.GET, entity, String.class);

        ObjectMapper mapper = new ObjectMapper();
        Map<String,Object> map = mapper.readValue(response.getBody(), Map.class);
        Map<String, Object> list = (Map<String, Object>) map.get("category_list");
        List<Map<String, Object>> categories = (List<Map<String, Object>>) list.get("categories");
        List<List<String>> categoriesList = new ArrayList<>();
        for(Map<String, Object> c : categories)
        {
            List<String> l = new ArrayList<>();
            l.add(c.get("name") + "\n");
            l.add("categoryId: " + c.get("id") + "\n");
            l.add("description: " + c.get("description") + "\n");
            l.add("url: " + DISCOURSE_RESPONSE_URL + c.get("topic_url") + "\n");
            categoriesList.add(l);
        }
        logger.info("Received categories: \n {}", categoriesList);
        // amazing string manipulation
        return String.valueOf(categoriesList).replaceAll("\\],", "\n").replaceAll("\\[", "").replaceAll("\\]", "").replaceAll(",", ":");
    }

    @RequestMapping(value = "/topics-by-category", method = RequestMethod.GET)
    public String getTopicsByCategory(@RequestParam(value="text")String categoryId) throws IOException {
        logger.info("Fetching topics by categy...");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(BASE_DISCOURSE_URL + "/c/" + categoryId + ".json");

        uriBuilder.queryParam("api_key", DISCOURSE_API_KEY);
        uriBuilder.queryParam("api_username", DISCOURSE_USR);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(uriBuilder.build().encode().toUri(), HttpMethod.GET, entity, String.class);
        ObjectMapper mapper = new ObjectMapper();
        Map<String,Object> map = mapper.readValue(response.getBody(), Map.class);
        Map<String, Object> list = (Map<String, Object>) map.get("topic_list");
        List<Map<String, Object>> topics = (List<Map<String, Object>>) list.get("topics");
        List<List<String>> topicsList = new ArrayList<>();
        for(Map<String, Object> t : topics)
        {
            List<String> l = new ArrayList<>();
            l.add(t.get("title") + "\n");
            l.add("url: " + BASE_DISCOURSE_URL + "/" + t.get("id") + "\n");
            topicsList.add(l);
        }
        logger.info("Received topics by category: \n {}", topicsList);

        // amazing string manipulation
        return String.valueOf(topicsList).replaceAll("\\],", "\n").replaceAll("\\[", "").replaceAll("\\]", "").replaceAll(",", ":");
    }

    private String getChannelId(String text) throws IOException {
        logger.info("Fetching channelId for: ", text);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(CHANNELS_URL);

        uriBuilder.queryParam("token", SLACK_API_TOKEN + SLACK_API_TOKEN2);

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

        logger.info("Received channelId[" + channelId + "]");
        return channelId;
    }

    private String getChannelsHistory(String channelId, List<Map<String, Object>> users) throws IOException {
        logger.info("Fetching channel history for channelId: ", channelId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(CHANNELS_HISTORY_URL);

        uriBuilder.queryParam("channel", channelId);
        uriBuilder.queryParam("token", SLACK_API_TOKEN + SLACK_API_TOKEN2);

        ResponseEntity<String> addressResponse = restTemplate.exchange(uriBuilder.build().encode().toUri(), HttpMethod.GET, entity, String.class);

        ObjectMapper mapper = new ObjectMapper();
        Map<String,Object> map = mapper.readValue(addressResponse.getBody(), Map.class);

        /**
         * logic to fetch messages between emojis
         */
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
                        String userName = "";
                        List<String> msg = new ArrayList<>();
                        for(Map<String, Object> u : users)
                        {
                            if(usr.equals(u.get("id")))
                            {
                                userName = (String) u.get("name");
                            }
                        }
                        if(c.containsKey("reactions"))
                        {
                            List<LinkedHashMap<String, Object>> reactions = (List<LinkedHashMap<String, Object>>) c.get("reactions");
                            for(LinkedHashMap<String, Object> r : reactions)
                            {
                                if(r.get("name").equals("star2"))
                                {
                                    msg.add(userName);
                                    msg.add(txt);
                                    msgResponse.add(0, msg);
                                    reactionFound = true;
                                }
                                if(r.get("name").equals("star"))
                                {
                                    msg.add(userName);
                                    msg.add(txt);
                                    msgResponse.add(0, msg);
                                    end = true;
                                }
                            }
                        }
                        else if(reactionFound)
                        {
                            msg.add(userName);
                            msg.add(txt);
                            msgResponse.add(0, msg);
                        }
                    }
                }
            }
        }
        logger.info("Received messages: \n {}", msgResponse);
        // amazing string manipulation
        return String.valueOf(msgResponse).replaceAll("\\],", "\n").replaceAll("\\[", "").replaceAll("\\]", "").replaceAll(",", ":");
    }

    private List<Map<String, Object>> getUsers() throws IOException {
        logger.info("Fetching users...");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(USERS_NAME_URL);

        uriBuilder.queryParam("token", SLACK_API_TOKEN + SLACK_API_TOKEN2);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> channelsList = restTemplate.exchange(uriBuilder.build().encode().toUri(), HttpMethod.GET, entity, String.class);

        ObjectMapper mapper = new ObjectMapper();
        Map<String,Object> map = mapper.readValue(channelsList.getBody(), Map.class);
        List<Map<String, Object>> members = (List<Map<String, Object>>) map.get("members");

        logger.info("Received users: \n {}", members);

        return members;

    }

    private String createTopic(String title, String body, String category) throws IOException {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(DISCOURSE_TOPIC_URL);

        uriBuilder.queryParam("api_key", DISCOURSE_API_KEY);
        uriBuilder.queryParam("api_username", DISCOURSE_USR);
        uriBuilder.queryParam("title", title);
        uriBuilder.queryParam("raw", body);
        uriBuilder.queryParam("category", category);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(uriBuilder.build().encode().toUri(), HttpMethod.POST, entity, String.class);

        ObjectMapper mapper = new ObjectMapper();
        Map<String,Object> map = mapper.readValue(response.getBody(), Map.class);

        return DISCOURSE_RESPONSE_URL + "title" + "/" + String.valueOf(map.get("id"));
    }
}
