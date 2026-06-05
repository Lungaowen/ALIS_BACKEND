package za.ac.alis.core.dto;

import java.util.ArrayList;
import java.util.List;

public class RagAnswerDTO {

    private String question;
    private String summary;
    private List<String> risks = new ArrayList<>();
    private List<String> recommendations = new ArrayList<>();
    private String answer;
    private boolean grounded;
    private List<RagSourceDTO> sources = new ArrayList<>();

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getRisks() {
        return risks;
    }

    public void setRisks(List<String> risks) {
        this.risks = risks;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public boolean isGrounded() {
        return grounded;
    }

    public void setGrounded(boolean grounded) {
        this.grounded = grounded;
    }

    public List<RagSourceDTO> getSources() {
        return sources;
    }

    public void setSources(List<RagSourceDTO> sources) {
        this.sources = sources;
    }
}
