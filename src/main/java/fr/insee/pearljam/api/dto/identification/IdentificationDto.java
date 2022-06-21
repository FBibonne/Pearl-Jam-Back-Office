package fr.insee.pearljam.api.dto.identification;

import fr.insee.pearljam.api.domain.Identification;
import fr.insee.pearljam.api.domain.IdentificationQuestions.AccessQuestionValue;
import fr.insee.pearljam.api.domain.IdentificationQuestions.CategoryQuestionValue;
import fr.insee.pearljam.api.domain.IdentificationQuestions.IdentificationQuestionValue;
import fr.insee.pearljam.api.domain.IdentificationQuestions.OccupantQuestionValue;
import fr.insee.pearljam.api.domain.IdentificationQuestions.SituationQuestionValue;

public class IdentificationDto {

    private Boolean move;

    private IdentificationQuestionValue identification;

    private AccessQuestionValue access;

    private SituationQuestionValue situation;

    private CategoryQuestionValue category;

    private OccupantQuestionValue occupant;

    public IdentificationDto() {
    }

    public IdentificationDto(Identification ident) {
        this.move = ident.isMove();
        this.identification = ident.getIdentification();
        this.access = ident.getAccess();
        this.situation = ident.getSituation();
        this.category = ident.getCategory();
        this.occupant = ident.getOccupant();
    }

    public Boolean isMove() {
        return this.move;
    }

    public Boolean getMove() {
        return this.move;
    }

    public void setMove(Boolean move) {
        this.move = move;
    }

    public IdentificationQuestionValue getIdentification() {
        return this.identification;
    }

    public void setIdentification(IdentificationQuestionValue identification) {
        this.identification = identification;
    }

    public AccessQuestionValue getAccess() {
        return this.access;
    }

    public void setAccess(AccessQuestionValue access) {
        this.access = access;
    }

    public SituationQuestionValue getSituation() {
        return this.situation;
    }

    public void setSituation(SituationQuestionValue situation) {
        this.situation = situation;
    }

    public CategoryQuestionValue getCategory() {
        return this.category;
    }

    public void setCategory(CategoryQuestionValue category) {
        this.category = category;
    }

    public OccupantQuestionValue getOccupant() {
        return this.occupant;
    }

    public void setOccupant(OccupantQuestionValue occupant) {
        this.occupant = occupant;
    }

}
