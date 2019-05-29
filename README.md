# PFE-Experiements	

## Environment
- **OS:** Ubuntu 18.04.2 LTS
- **CPU:** Intel XEON E3-1270 v3 @ 3.50Ghz x 8
- **RAM:** 32GiB
- **Java Runtime:** OpenJDK 1.8.0_212

## List of repeatable experiments
**AES**
- aes_global_round_mc.yml

### AES
#### AES Global Round MC
**Description:**
Implementation of AES problem with the abstract constraint *abstract XOR*. The problem is solved in two step.
- Step 1: We are looking for the number of active SBoxes by round
- Step 2: We solve the problem for each step 1

MixColumn during step 1 is implemented using DZ = A dot DY xor B dot DY2 xor C dot DY3 with the corresponding coefficients of the MixColumn matrix.
