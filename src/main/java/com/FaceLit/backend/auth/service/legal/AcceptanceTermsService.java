package com.FaceLit.backend.auth.service.legal;

import com.FaceLit.backend.auth.dto.request.legal.AcceptanceTermsRequestDTO;
import com.FaceLit.backend.auth.dto.response.legal.AcceptanceTermsResponseDTO;

public interface AcceptanceTermsService {

    // cuando acepta los terminos recibe los datos aceptados         // Es String porque una dirección IP se maneja como texto en Java.
    AcceptanceTermsResponseDTO acceptanceTerms(AcceptanceTermsRequestDTO dto);

}
