package com.coworkinghub.coworkinghub.dto;

import com.coworkinghub.coworkinghub.model.AccountType;
import com.coworkinghub.coworkinghub.model.Compte;

/** What other users may see of an account: no email. */
public record PublicProfile(Long id, String name, String surname, Integer age, String profession,
                            AccountType typeCompte) {
    public static PublicProfile of(Compte c) {
        return new PublicProfile(c.getId(), c.getName(), c.getSurname(), c.getAge(), c.getProfession(),
                c.getTypeCompte());
    }
}
