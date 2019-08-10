<script type="text/javascript" async="" src="https://cdnjs.cloudflare.com/ajax/libs/mathjax/2.7.2/MathJax.js?config=TeX-MML-AM_CHTML"></script>

# PFE-Experiements	

## Prerequisites
- **Gradle**: Gralde 4.4.1 or later
- **Java Runtime:** OpenJDK 1.8.0_212 or later

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
- aes_advanced_round_no_transit_then_global.yml
- aes_advanced_round_transit_then_global.yml

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

#### AES Advanced Round No Transit Then Global
**Description:**
Use the Advanced Model to solve the $$step_{round}$$ without the diff variables (no transitivity). Then use the Global model to solve the $$step_{1}$$.

#### AES Advanced Round Transit Then Global
**Description:**
Use the Advanced Model to solve the $$step_{round}$$ **WITH** the diff variables (transitivity). Then use the Global model to solve the $$step_{1}$$.

#### AES Advanced Round Transit Then Global

### Midori
#### Midori Full Steps
**Description:**
We solve the $$step_{round}$$, the $$step_{1}$$ with the global constraint *abstract XOR*. The realisation step is implemented by constraint tables.
