# PFE-Experiements	

## Environment
- **OS:** Ubuntu 18.04.2 LTS
- **CPU:** Intel XEON E3-1270 v3 @ 3.50Ghz x 8
- **RAM:** 32GiB
- **Java Runtime:** OpenJDK 1.8.0_212

## List of repeatable experiments
**AES**
- aes_global_round_mc.yml
- aes_global_round_no_mc.yml
- aes_advanced_round_no_transit.yml
- aes_advanced_round_transit.yml

**Midori**
- midori_full_steps.yml

### AES
#### AES Global Round MC
**Description:**
Implementation of AES problem with the abstract constraint *abstract XOR*. The problem is solved in two step.
- Step 1: We are looking for the number of active SBoxes by round
- Step 2: We solve the problem for each step 1

MixColumn during step 1 is implemented using DZ = (A $$\cdot$$ DY) $$\oplus$$ (B $$\cdot$$ DY2) $$\oplus$$ (C $$\cdot$$ DY3) with the corresponding coefficients of the MixColumn matrix.

#### AES Global Round No MC
**Description:**
Implementation of AES problem with the abstract constraint *abstract XOR*. The problem is solved in two step.

- Step 1: We are looking for the number of active SBoxes by round
- Step 2: We solve the problem for each step 1

MixColumn during step 1 is implemented diff variables to represents the maximum distance separable property of the mix columns matrix.

#### AES Advanced Round No Transit
**Description:**
Solving the $$step_{round}$$ and the $$step_{1}$$ with Advanced Model, the transitivity constraints over DY and DK are **disabled**.

#### AES Advanced Round Transit
**Description:**
Solving the $$step_{round}$$ and the s$$tep_{1}$$ with Advanced Model, the transitivity constraints over DY and DK are **enabled**.

### Midori
#### Midori Full Steps
**Description:**
We solve the $$step_{round}$$, the $$step_{1}$$ with the global constraint *abstract XOR*. The realisation step is implemented by constraint tables.