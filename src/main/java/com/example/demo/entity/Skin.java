package com.example.demo.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "skins")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Skin {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(nullable = false, unique = true, length = 50)
    private String code;
    @Column(name = "primary_color", nullable = false, length = 7)
    private String primaryColor;
    @Column(name = "secondary_color", nullable = false, length = 7)
    private String secondaryColor;
    @Column(name = "background_color", nullable = false, length = 7)
    private String backgroundColor;
    @Column(name = "text_color", nullable = false, length = 7)
    private String textColor;
    @Column(name = "preview_url", length = 500)
    private String previewUrl;
    @Column(name = "background_image_url", length = 500)
    private String backgroundImageUrl;
    @Column(name = "is_default", nullable = false)
    @JsonProperty("isDefault")
    private boolean isDefault;
    @Column(name = "is_active", nullable = false)
    @JsonProperty("isActive")
    private boolean isActive;
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
