package com.ohmyimage.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Board {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @ManyToOne(fetch = FetchType.LAZY) // FetchType.LAZY genellikle daha iyi
    @JoinColumn(name = "owner_id", nullable = true) //ullable = true
    private User owner;

    // CascadeType.ALL yerine orphanRemoval=true kullanmak daha güvenli ama pinleri silmek istemediğimiz için CascadeType.ALL'u kaldırabiliriz
    @OneToMany(mappedBy = "board", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    @Builder.Default
    private Set<Pin> pins = new HashSet<>();
}