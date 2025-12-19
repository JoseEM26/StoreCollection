package com.proyecto.StoreCollection.dto.DropTown;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AtributoDropdownDTO extends DropTownStandar {
    private List<DropTownStandar> valores = new ArrayList<>();
}