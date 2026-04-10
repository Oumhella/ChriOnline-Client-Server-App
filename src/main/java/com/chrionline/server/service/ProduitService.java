package com.chrionline.server.service;

import com.chrionline.database.DatabaseConnection;
import com.chrionline.server.dao.ProduitDAO;
import com.chrionline.shared.models.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProduitService {

    public Map<String, Object> handleListeProduits(Map<String, Object> req) {
        Map<String, Object> reponse = new HashMap<>();
        try {
            List<Produit> produits = ProduitDAO.findAll();
            reponse.put("statut", "OK");
            reponse.put("produits", produits);
        } catch (Exception e) {
            reponse.put("statut", "ERREUR");
            reponse.put("message", e.getMessage());
        }
        return reponse;
    }

    public Map<String, Object> handleGetProduitById(Map<String, Object> req) {
        System.out.println("[ProduitService] Reçu handleGetProduitById : " + req);
        Map<String, Object> reponse = new HashMap<>();

        try {
            Object idObj = req.get("id");

            if (idObj == null) {
                System.err.println("[ProduitService] ID manquant dans la requête !");
                reponse.put("statut", "ERREUR");
                reponse.put("message", "ID manquant");
                return reponse;
            }

            int id = Integer.parseInt(idObj.toString());
            System.out.println("[ProduitService] Recherche produit ID=" + id + "...");
            Produit produit = ProduitDAO.findById(id);

            if (produit != null) {
                System.out.println("[ProduitService] Produit trouvé: " + produit.getNom());
                reponse.put("statut", "OK");
                reponse.put("produit", produit);
            } else {
                System.err.println("[ProduitService] Produit NON trouvé ID=" + id);
                reponse.put("statut", "NOT_FOUND");
                reponse.put("message", "Produit introuvable");
            }

        } catch (Exception e) {
            reponse.put("statut", "ERREUR");
            reponse.put("message", e.getMessage());
        }

        return reponse;
    }

    public Map<String, Object> handleAjouterProduit(Map<String, Object> req) {
        System.out.println("[ProduitService] Requête AjouterProduit reçue : " + req.get("nom"));
        Map<String, Object> reponse = new HashMap<>();
        try {
            Produit p = mapToProduit(req);
            System.out.println("[ProduitService] Mapping terminé, appel DAO insert...");
            int id = ProduitDAO.insert(p);
            if (id > 0) {
                System.out.println("[ProduitService] Succès insertion ID=" + id);
                reponse.put("statut", "OK");
                reponse.put("id", id);
                reponse.put("message", "Produit ajouté avec succès.");
            } else {
                System.err.println("[ProduitService] Échec insertion DAO (ID <= 0)");
                reponse.put("statut", "ERREUR");
                reponse.put("message", "Échec de l'ajout en base de données.");
            }
        } catch (Exception e) {
            System.err.println("[ProduitService] Erreur lors du traitement : " + e.getMessage());
            e.printStackTrace();
            reponse.put("statut", "ERREUR");
            reponse.put("message", e.getMessage());
        }
        return reponse;
    }

    public Map<String, Object> handleModifierProduit(Map<String, Object> req) {
        System.out.println("[ProduitService] Requête ModifierProduit reçue pour ID=" + req.get("id_produit"));
        Map<String, Object> reponse = new HashMap<>();
        try {
            Produit p = mapToProduit(req);
            System.out.println("[ProduitService] Mapping terminé. Nombre de formats: " + p.getFormats().size());
            boolean success = ProduitDAO.update(p);
            if (success) {
                reponse.put("statut", "OK");
                reponse.put("message", "Produit modifié avec succès.");
            } else {
                reponse.put("statut", "ERREUR");
                reponse.put("message", "Échec de la modification.");
            }
        } catch (Exception e) {
            reponse.put("statut", "ERREUR");
            reponse.put("message", e.getMessage());
        }
        return reponse;
    }

    public Map<String, Object> handleSupprimerProduit(Map<String, Object> req) {
        Map<String, Object> reponse = new HashMap<>();
        try {
            int id = Integer.parseInt(req.get("id_produit").toString());
            boolean success = ProduitDAO.delete(id);
            if (success) {
                reponse.put("statut", "OK");
                reponse.put("message", "Produit supprimé avec succès.");
            } else {
                reponse.put("statut", "ERREUR");
                reponse.put("message", "Échec de la suppression.");
            }
        } catch (Exception e) {
            reponse.put("statut", "ERREUR");
            reponse.put("message", e.getMessage());
        }
        return reponse;
    }

    public Map<String, Object> handleUploadImage(Map<String, Object> req) {
        Map<String, Object> reponse = new HashMap<>();
        try {
            byte[] data = (byte[]) req.get("data");
            String ext = (String) req.get("extension");
            String path = ImageUploadService.saveImage(data, ext);
            if (path != null) {
                reponse.put("statut", "OK");
                reponse.put("url", path);
            } else {
                reponse.put("statut", "ERREUR");
                reponse.put("message", "Échec de la sauvegarde image.");
            }
        } catch (Exception e) {
            reponse.put("statut", "ERREUR");
            reponse.put("message", e.getMessage());
        }
        return reponse;
    }

    public Map<String, Object> handleListeCategories(Map<String, Object> req) {
        Map<String, Object> res = new HashMap<>();
        res.put("statut", "OK");
        res.put("categories", ProduitDAO.findAllCategories());
        return res;
    }

    public Map<String, Object> handleListeLabels(Map<String, Object> req) {
        Map<String, Object> res = new HashMap<>();
        int idCat = Integer.parseInt(req.get("id_categorie").toString());
        res.put("statut", "OK");
        res.put("labels", ProduitDAO.findLabelsByCategorie(idCat));
        return res;
    }

    public Map<String, Object> handleListeLabelValues(Map<String, Object> req) {
        Map<String, Object> res = new HashMap<>();
        int idLabel = Integer.parseInt(req.get("id_label").toString());
        res.put("statut", "OK");
        res.put("values", ProduitDAO.findLabelValuesByLabel(idLabel));
        return res;
    }

    public Map<String, Object> handleAjouterLabel(Map<String, Object> req) {
        Map<String, Object> res = new HashMap<>();
        Label l = new Label();
        l.setNom((String) req.get("nom"));
        l.setIdCategorie(Integer.parseInt(req.get("id_categorie").toString()));
        int id = ProduitDAO.insertLabel(l);
        res.put("statut", id > 0 ? "OK" : "ERREUR");
        res.put("id", id);
        return res;
    }

    public Map<String, Object> handleAjouterLabelValue(Map<String, Object> req) {
        Map<String, Object> res = new HashMap<>();
        LabelValue lv = new LabelValue();
        lv.setValeur((String) req.get("valeur"));
        Label l = new Label();
        l.setId(Integer.parseInt(req.get("id_label").toString()));
        lv.setLabel(l);
        int id = ProduitDAO.insertLabelValue(lv);
        res.put("statut", id > 0 ? "OK" : "ERREUR");
        res.put("id", id);
        return res;
    }

    public Map<String, Object> handleSupprimerLabelValue(Map<String, Object> req) {
        Map<String, Object> res = new HashMap<>();
        int id = Integer.parseInt(req.get("id_labelValues").toString());
        boolean success = ProduitDAO.deleteLabelValue(id);
        res.put("statut", success ? "OK" : "ERREUR");
        return res;
    }

    public Map<String, Object> handleAjouterCategorie(Map<String, Object> req) {
        Map<String, Object> res = new HashMap<>();
        try {
            Categorie c = new Categorie();
            c.setNom((String) req.get("nom"));
            c.setDescription((String) req.get("description"));
            Object parentId = req.get("id_parent");
            if (parentId != null) c.setIdParent(Integer.parseInt(parentId.toString()));
            
            int id = ProduitDAO.insertCategorie(c);
            res.put("statut", id > 0 ? "OK" : "ERREUR");
            res.put("id", id);
        } catch (Exception e) {
            res.put("statut", "ERREUR");
            res.put("message", e.getMessage());
        }
        return res;
    }

    public Map<String, Object> handleModifierCategorie(Map<String, Object> req) {
        Map<String, Object> res = new HashMap<>();
        try {
            Categorie c = new Categorie();
            c.setId(Integer.parseInt(req.get("id_categorie").toString()));
            c.setNom((String) req.get("nom"));
            c.setDescription((String) req.get("description"));
            Object parentId = req.get("id_parent");
            if (parentId != null) c.setIdParent(Integer.parseInt(parentId.toString()));
            
            boolean success = ProduitDAO.updateCategorie(c);
            res.put("statut", success ? "OK" : "ERREUR");
        } catch (Exception e) {
            res.put("statut", "ERREUR");
            res.put("message", e.getMessage());
        }
        return res;
    }

    public Map<String, Object> handleSupprimerCategorie(Map<String, Object> req) {
        Map<String, Object> res = new HashMap<>();
        try {
            int id = Integer.parseInt(req.get("id_categorie").toString());
            boolean success = ProduitDAO.deleteCategorie(id);
            res.put("statut", success ? "OK" : "ERREUR");
        } catch (Exception e) {
            res.put("statut", "ERREUR");
            res.put("message", e.getMessage());
        }
        return res;
    }

    public Map<String, Object> handleApplyDiscountCategorie(Map<String, Object> req) {
        Map<String, Object> res = new HashMap<>();
        try {
            int id = Integer.parseInt(req.get("id_categorie").toString());
            double discount = Double.parseDouble(req.get("discount").toString());
            boolean success = ProduitDAO.applyDiscount(id, discount);
            // Also update the category discount percentage directly if success? Or the user can just see it.
            // Let's also update the category's discount field for future reference.
            if(success) {
               Categorie c = null;
               List<Categorie> cats = ProduitDAO.findAllCategories();
               for(Categorie iter : cats) {
                   if(iter.getId() == id) { c = iter; break; }
               }
               if(c != null) {
                   c.setDiscount(discount);
                   ProduitDAO.updateCategorie(c);
               }
            }
            res.put("statut", success ? "OK" : "ERREUR");
        } catch (Exception e) {
            res.put("statut", "ERREUR");
            res.put("message", e.getMessage());
        }
        return res;
    }

    @SuppressWarnings("unchecked")
    private Produit mapToProduit(Map<String, Object> map) {
        Produit p = new Produit();
        if (map.get("id_produit") != null) p.setIdProduit(Integer.parseInt(map.get("id_produit").toString()));
        p.setNom((String) map.get("nom"));
        p.setDescription((String) map.get("description"));
        p.setIdCategorie(Integer.parseInt(map.getOrDefault("id_categorie", "1").toString()));

        List<Map<String, Object>> formatsMap = (List<Map<String, Object>>) map.get("formats");
        if (formatsMap != null) {
            System.out.println("[ProduitService] Mapping de " + formatsMap.size() + " formats...");
            List<ProductFormat> formats = new ArrayList<>();
            for (Map<String, Object> fm : formatsMap) {
                ProductFormat pf = new ProductFormat();
                Object fid = fm.get("id_product_formats");
                if (fid != null) {
                    int idFmt = Integer.parseInt(fid.toString());
                    System.out.println("[ProduitService] -> Format ID detecté: " + idFmt);
                    pf.setId(idFmt);
                }
                pf.setPrix(Double.parseDouble(fm.get("prix").toString()));
                pf.setStock(Integer.parseInt(fm.get("stock").toString()));
                pf.setStockAlerte(Integer.parseInt(fm.get("stock_alerte").toString()));
                pf.setImageUrl((String) fm.get("image_url"));
                
                List<Integer> lvIds = (List<Integer>) fm.get("labelValueIds");
                if (lvIds != null) {
                    System.out.println("[ProduitService] --> " + lvIds.size() + " variantes détectées pour ce format.");
                    List<LabelValue> lvs = new ArrayList<>();
                    for (Integer lvId : lvIds) {
                        LabelValue lv = new LabelValue();
                        lv.setId(lvId);
                        lvs.add(lv);
                    }
                    pf.setLabelValues(lvs);
                }
                formats.add(pf);
            }
            p.setFormats(formats);
        }
        return p;
    }
}