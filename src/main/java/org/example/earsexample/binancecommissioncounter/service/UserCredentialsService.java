package org.example.earsexample.binancecommissioncounter.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Service
@Data
@NoArgsConstructor
@AllArgsConstructor
@Component
public class UserCredentialsService {

    private String apiKey;
    private String secretKey;

}