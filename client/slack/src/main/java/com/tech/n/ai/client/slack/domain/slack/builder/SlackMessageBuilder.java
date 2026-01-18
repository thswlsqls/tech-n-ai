package com.tech.n.ai.client.slack.domain.slack.builder;

import com.tech.n.ai.client.slack.domain.slack.contract.SlackDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Slack Block Kit 메시지 빌더
 * Builder 패턴을 사용하여 Block Kit 메시지 구성
 */
public class SlackMessageBuilder {
    
    private final List<Map<String, Object>> blocks = new ArrayList<>();
    
    /**
     * Section Block 추가
     * 
     * @param text 표시할 텍스트 (Markdown 지원)
     * @return 빌더 인스턴스
     */
    public SlackMessageBuilder addSection(String text) {
        Map<String, Object> block = new HashMap<>();
        block.put("type", "section");
        
        Map<String, Object> textObj = new HashMap<>();
        textObj.put("type", "mrkdwn");
        textObj.put("text", text);
        block.put("text", textObj);
        
        blocks.add(block);
        return this;
    }
    
    /**
     * Divider Block 추가
     * 
     * @return 빌더 인스턴스
     */
    public SlackMessageBuilder addDivider() {
        Map<String, Object> block = new HashMap<>();
        block.put("type", "divider");
        blocks.add(block);
        return this;
    }
    
    /**
     * Context Block 추가
     * 보조 정보를 작은 텍스트로 표시
     * 
     * @param text 표시할 텍스트 (Markdown 지원)
     * @return 빌더 인스턴스
     */
    public SlackMessageBuilder addContext(String text) {
        Map<String, Object> block = new HashMap<>();
        block.put("type", "context");
        
        List<Map<String, Object>> elements = new ArrayList<>();
        Map<String, Object> textObj = new HashMap<>();
        textObj.put("type", "mrkdwn");
        textObj.put("text", text);
        elements.add(textObj);
        block.put("elements", elements);
        
        blocks.add(block);
        return this;
    }
    
    /**
     * 최종 SlackMessage 생성
     * 
     * @return 구성된 SlackMessage
     */
    public SlackDto.SlackMessage build() {
        // Block을 SlackDto.Block으로 변환
        List<SlackDto.Block> blockList = blocks.stream()
            .map(blockMap -> {
                SlackDto.Block.BlockBuilder builder = SlackDto.Block.builder()
                    .type((String) blockMap.get("type"));
                
                if (blockMap.containsKey("text")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> textMap = (Map<String, Object>) blockMap.get("text");
                    builder.text(textMap);
                }
                
                if (blockMap.containsKey("elements")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> elementsList = (List<Map<String, Object>>) blockMap.get("elements");
                    builder.elements(elementsList);
                }
                
                return builder.build();
            })
            .toList();
        
        return SlackDto.SlackMessage.builder()
            .blocks(blockList)
            .build();
    }
}
