%RWF=/scratch/Gau-12-ethanediol/,32GB
%Nosave
%Chk=12-ethanediol-opt-5-1-2-6-223.chk
%Mem=389242KB
%Nproc=1
#HF/6-31G* Opt=ModRed MaxDisk=32GB

12-ethanediol Rotatable Bond Optimization on node9.bme.utexas.edu

0 1
 C   -0.429680    0.623579    0.069804
 C    0.429680   -0.623579    0.069804
 H   -0.068690    2.525587   -0.033356
 H    0.068690   -2.525587   -0.033356
 O    0.429680    1.722518   -0.077625
 O   -0.429680   -1.722518   -0.077625
 H   -1.131666    0.558761   -0.756915
 H   -1.001184    0.682361    0.992446
 H    1.131666   -0.558761   -0.756915
 H    1.001184   -0.682361    0.992446

5 1 2 6     222.64 F

