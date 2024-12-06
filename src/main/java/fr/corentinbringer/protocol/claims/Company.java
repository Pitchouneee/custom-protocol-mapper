package fr.corentinbringer.protocol.claims;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Company {

    private String name;
    private String label;
}
