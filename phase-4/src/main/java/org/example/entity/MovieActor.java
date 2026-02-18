package org.example.entity;

import java.util.List;

/**
 * 电影演员实体类 - 用于演示 BeanOutputConverter
 * 
 * 结构化输出转换器可以将 AI 的 JSON 响应自动映射到此 Java 类
 * 
 * @author Spring AI Course
 */
public class MovieActor {

    /**
     * 演员姓名
     */
    private String actor;

    /**
     * 演员出演的电影列表
     */
    private List<String> movies;

    /**
     * 演员获得的奖项列表
     */
    private List<String> awards;

    public MovieActor() {
    }

    public MovieActor(String actor, List<String> movies, List<String> awards) {
        this.actor = actor;
        this.movies = movies;
        this.awards = awards;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public List<String> getMovies() {
        return movies;
    }

    public void setMovies(List<String> movies) {
        this.movies = movies;
    }

    public List<String> getAwards() {
        return awards;
    }

    public void setAwards(List<String> awards) {
        this.awards = awards;
    }

    @Override
    public String toString() {
        return "MovieActor{" +
                "actor='" + actor + '\'' +
                ", movies=" + movies +
                ", awards=" + awards +
                '}';
    }
}
