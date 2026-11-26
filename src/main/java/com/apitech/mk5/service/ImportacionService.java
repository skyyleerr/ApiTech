package com.apitech.mk5.service;

import com.apitech.mk5.dto.response.ImportacionResultadoDTO;
import org.springframework.web.multipart.MultipartFile;

public interface ImportacionService {
    ImportacionResultadoDTO previsualizar(MultipartFile archivo);
    ImportacionResultadoDTO confirmar(ImportacionResultadoDTO vistaPrevia);
}
